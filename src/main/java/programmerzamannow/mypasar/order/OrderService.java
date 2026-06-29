package programmerzamannow.mypasar.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import programmerzamannow.mypasar.audit.AuditLogRepository;
import programmerzamannow.mypasar.order.dto.*;
import programmerzamannow.mypasar.order.entity.Order;
import programmerzamannow.mypasar.order.entity.OrderDetail;
import programmerzamannow.mypasar.product.entity.Product;
import programmerzamannow.mypasar.product.repository.ProductRepository;
import programmerzamannow.mypasar.shared.validation.ValidationService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private ValidationService validationService;

        @Autowired
        private AuditLogRepository auditLogRepository;

        @Autowired
        private StringRedisTemplate redisTemplate;

        private OrderResponseDto mapToResponse(Order order, List<OrderDetailResponseDto> items) {
                return OrderResponseDto.builder()
                                .orderId(order.getId())
                                .username(order.getUsername())
                                .orderDate(order.getOrderDate())
                                .totalAmount(order.getTotalAmount())
                                .status(order.getStatus())
                                .items(items)
                                .build();
        }

        @Transactional
        public OrderResponseDto createOrder(String currentUsername, OrderRequestDto request) {

                validationService.validate(request);
                List<String> productIds = request.getItems().stream()
                                .map(OrderItemDto::getProductId)
                                .collect(Collectors.toList());

                List<Product> productsFromDb = productRepository.findAllById(productIds);
                Map<String, Product> productMap = productsFromDb.stream()
                                .collect(Collectors.toMap(Product::getId, p -> p));

                for (String id : productIds) {
                        if (!productMap.containsKey(id)) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Product tidak ditemukan: " + id);
                        }
                }

                long waktuSekarang = System.currentTimeMillis();

                Order order = Order.builder()
                                .id(UUID.randomUUID().toString())
                                .username(currentUsername)
                                .orderDate(waktuSekarang)
                                .status("PENDING")
                                .totalAmount(BigDecimal.ZERO)
                                .expiredAt(waktuSekarang + 1000 * 60 * 30)
                                .build();

                List<OrderDetail> orderDetails = new ArrayList<>();
                List<OrderDetailResponseDto> detailResponses = new ArrayList<>();
                BigDecimal totalAmount = BigDecimal.ZERO;

                // 🔄 Tracker untuk mencatat produk mana saja yang sukses mengubah Redis
                Map<String, Integer> successfulIncrements = new HashMap<>();

                try {
                        // LOOPING VALIDASI STOK DENGAN METODE ATOMIC INCREMENT
                        // 🔥 LOOPING VALIDASI STOK YANG SUDAH DIPERBAIKI (BERSIH DARI BUG)
                        for (OrderItemDto itemDto : request.getItems()) {
                                Product product = productMap.get(itemDto.getProductId());
                                String userBookingKey = "booking:user:" + product.getId() + ":" + currentUsername;
                                String globalBookingKey = "booking:total:product:" + product.getId();

                                // 1. Langsung kunci kuota di Redis secara atomik (Anti Race Condition)
                                Long newGlobalTotal = redisTemplate.opsForValue().increment(globalBookingKey,
                                                (long) itemDto.getQuantity());
                                redisTemplate.expire(globalBookingKey, Duration.ofMinutes(10));

                                // Track untuk rollback jika produk selanjutnya gagal
                                successfulIncrements.put(globalBookingKey, itemDto.getQuantity());

                                // 2. VALIDASI SEDERHANA & AKURAT: Jika total booking di Redis melebihi stok di
                                // MySQL, langsung REJECT!
                                if (newGlobalTotal.intValue() > product.getStock()) {
                                        int sisaStokAsli = product.getStock()
                                                        - (newGlobalTotal.intValue() - itemDto.getQuantity());
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "Stok " + product.getName()
                                                                        + " tidak mencukupi. Sisa stok yang tersedia: "
                                                                        + Math.max(0, sisaStokAsli));
                                }

                                // 3. Jika lolos, catat/tambahkan kuantitas ke booking pribadi user di Redis
                                // Gunakan increment juga untuk user booking agar jika ada draf lain tidak
                                // saling menimpa
                                redisTemplate.opsForValue().increment(userBookingKey, (long) itemDto.getQuantity());
                                redisTemplate.expire(userBookingKey, Duration.ofMinutes(10));

                                // Kalkulasi harga seperti biasa
                                BigDecimal itemPrice = product.getPrice();
                                BigDecimal subTotal = itemPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                                totalAmount = totalAmount.add(subTotal);

                                orderDetails.add(OrderDetail.builder()
                                                .id(UUID.randomUUID().toString())
                                                .order(order)
                                                .product(product)
                                                .quantity(itemDto.getQuantity())
                                                .price(itemPrice)
                                                .build());

                                detailResponses.add(OrderDetailResponseDto.builder()
                                                .productId(product.getId())
                                                .productName(product.getName())
                                                .quantity(itemDto.getQuantity())
                                                .price(itemPrice)
                                                .build());
                        }
                } catch (Exception e) {
                        // 🔄 ATOMIC ROLLBACK REDIS: Jika ada satu saja produk yang gagal, kembalikan
                        // isi Redis ke posisi semula!
                        successfulIncrements.forEach((globalKey, quantity) -> {
                                redisTemplate.opsForValue().decrement(globalKey, (long) quantity);

                                // Ekstrak product ID dari global key untuk membersihkan user key-nya juga
                                String productId = globalKey.replace("booking:total:product:", "");
                                String userBookingKey = "booking:user:" + productId + ":" + currentUsername;
                                redisTemplate.opsForValue().decrement(userBookingKey, (long) quantity);
                        });
                        throw e;
                }

                // 5. JIKA SEMUA PRODUK LOLOS LOOPING, BARU SIMPAN KE MYSQL
                order.setTotalAmount(totalAmount);
                order.setOrderDetails(orderDetails);
                orderRepository.save(order); // Tersimpan aman ke database secara sinkron!

                return mapToResponse(order, detailResponses);
        }

        @Scheduled(fixedRate = 1000 * 60 * 30) // Berjalan otomatis setiap 2 menit
        @Transactional
        public void cleanExpiredOrdersInDb() {
                log.info("Mulai membersihkan order expired...");
                long now = System.currentTimeMillis();
                // Hapus semua order yang statusnya masih PENDING dan waktunya sudah melewati
                // batas
                orderRepository.deleteByStatusAndExpiredAtLessThan("PENDING", now);
        }

        @Transactional(readOnly = true) // 🌟 Best Practice: Gunakan readOnly untuk mempercepat kueri SELECT
        public OrderResponseDto getOrderDetail(String orderId) {
                // 1. Cari data induk Order berdasarkan ID
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order not found with ID: " + orderId));

                // 2. Konversi daftar isi barang (List<OrderDetail>) menjadi bentuk DTO Response
                List<OrderDetailResponseDto> itemResponses = order.getOrderDetails().stream()
                                .map(detail -> OrderDetailResponseDto.builder()
                                                .productId(detail.getProduct().getId())
                                                .productName(detail.getProduct().getName()) // Mengambil nama produk
                                                                                            // dari tabel relasi
                                                .quantity(detail.getQuantity())
                                                .price(detail.getPrice()) // Harga terkunci saat transaksi terjadi
                                                .build())
                                .collect(Collectors.toList());

                // 3. Satukan semuanya ke dalam bentuk OrderResponseDto
                return OrderResponseDto.builder()
                                .orderId(order.getId())
                                .username(order.getUsername())
                                .orderDate(order.getOrderDate())
                                .totalAmount(order.getTotalAmount())
                                .status(order.getStatus())
                                .items(itemResponses)
                                .build();
        }

        @Transactional
        public OrderResponseDto batchUpdateCartQuantities(String currentUsername, String orderId,
                        UpdateCartRequestDto request) {
                // 1. Ambil data Order induk di MySQL
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order tidak ditemukan"));

                if (!order.getStatus().equals("PENDING")) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keranjang sudah dikunci/checkout.");
                }

                // 2. Kumpulkan semua Product ID yang mau di-update oleh user
                List<String> productIds = request.getItems().stream()
                                .map(OrderItemDto::getProductId)
                                .collect(Collectors.toList());

                // 3. BATCH FETCH MYSQL: Ambil semua data produk sekaligus (Hanya 1 kueri)
                List<Product> productsFromDb = productRepository.findAllById(productIds);
                java.util.Map<String, Product> productMap = productsFromDb.stream()
                                .collect(Collectors.toMap(Product::getId, p -> p));

                // Ubah detail order MySQL menjadi Map agar mudah dicocokkan berdasarkan ID
                // produk
                java.util.Map<String, OrderDetail> detailMap = order.getOrderDetails().stream()
                                .collect(Collectors.toMap(detail -> detail.getProduct().getId(), detail -> detail));

                // Tracker untuk membatalkan (rollback) nilai Redis jika di tengah loop terjadi
                // kegagalan stok
                java.util.Map<String, Integer> successfulIncrements = new java.util.HashMap<>();

                // 4. LOOPING VALIDASI DAN UPDATE DENGAN ATOMIC INCREMENT YANG DISEDERHANAKAN ⚔️
                try {
                        for (int i = 0; i < request.getItems().size(); i++) {
                                OrderItemDto itemDto = request.getItems().get(i);
                                Product product = productMap.get(itemDto.getProductId());
                                OrderDetail targetDetail = detailMap.get(itemDto.getProductId());

                                if (product == null || targetDetail == null) {
                                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Produk tidak valid di keranjang ini");
                                }

                                int newQuantity = itemDto.getQuantity();
                                int oldQuantity = targetDetail.getQuantity();
                                int delta = newQuantity - oldQuantity; // 📊 Selisih kuantitas baru vs lama

                                String userBookingKey = "booking:user:" + product.getId() + ":" + currentUsername;
                                String globalBookingKey = "booking:total:product:" + product.getId();

                                // 🔥 LANGKAH ATOMIK: Langsung sesuaikan global total di Redis sebesar delta
                                // (bisa positif/negatif)
                                Long newGlobalTotal = redisTemplate.opsForValue().increment(globalBookingKey,
                                                (long) delta);
                                redisTemplate.expire(globalBookingKey, Duration.ofMinutes(10)); // Set TTL 10 Menit

                                // Catat track delta untuk rollback jika ada loop berikutnya yang gagal
                                successfulIncrements.put(globalBookingKey, delta);

                                // 🔥 VALIDASI SEDERHANA & SAKTI: Jika total booking baru di Redis melampaui
                                // stok asli MySQL, REJECT!
                                if (newGlobalTotal.intValue() > product.getStock()) {
                                        int sisaStokAsli = product.getStock() - (newGlobalTotal.intValue() - delta);
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "Stok " + product.getName()
                                                                        + " tidak mencukupi. Sisa stok tersedia: "
                                                                        + Math.max(0, sisaStokAsli));
                                }

                                // Jika lolos validasi, perbarui kuota draf pribadi milik user di Redis
                                redisTemplate.opsForValue().set(userBookingKey, String.valueOf(newQuantity),
                                                Duration.ofMinutes(10));

                                // UPDATE DETAIL QUANTITY DI MEMORI JAVA
                                targetDetail.setQuantity(newQuantity);
                        }
                } catch (Exception e) {
                        // 🔄 ATOMIC ROLLBACK: Jika salah satu barang gagal, kembalikan semua perubahan
                        // delta Redis ke posisi awal!
                        successfulIncrements.forEach((key, delta) -> {
                                redisTemplate.opsForValue().decrement(key, (long) delta);
                        });
                        throw e; // Lemparkan eror agar transaksi database MySQL ikut rollback
                }

                // 5. HITUNG ULANG TOTAL HARGA DAN SIMPAN SEKALIGUS KE MYSQL
                BigDecimal totalAmount = order.getOrderDetails().stream()
                                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                order.setTotalAmount(totalAmount);

                orderRepository.save(order); // 🔥 1 Kali Simpan permanen ke MySQL

                // Susun response DTO
                List<OrderDetailResponseDto> detailResponses = order.getOrderDetails().stream()
                                .map(detail -> OrderDetailResponseDto.builder()
                                                .productId(detail.getProduct().getId())
                                                .productName(detail.getProduct().getName())
                                                .quantity(detail.getQuantity())
                                                .price(detail.getPrice())
                                                .build())
                                .collect(Collectors.toList());

                return OrderResponseDto.builder()
                                .orderId(order.getId())
                                .username(order.getUsername())
                                .orderDate(order.getOrderDate())
                                .totalAmount(order.getTotalAmount())
                                .status(order.getStatus())
                                .items(detailResponses)
                                .build();
        }

        @Transactional
        public OrderResponseDto checkoutOrder(String currentUsername, String orderId) {
                // 1. Ambil data order dari MySQL
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order tidak ditemukan"));

                // Pastikan order belum pernah di-checkout sebelumnya
                if (!order.getStatus().equals("PENDING")) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Order ini sudah diproses atau dibatalkan.");
                }

                // ⏱️ PROTEKSI INSTAN 1: Validasi Kedaluwarsa Waktu di MySQL (Skenario 2 Menit)
                if (System.currentTimeMillis() > order.getExpiredAt()) {
                        order.setStatus("CANCELLED");
                        orderRepository.save(order);
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Maaf, waktu checkout keranjangmu sudah habis (Kedaluwarsa di MySQL).");
                }

                // ⏱️ PROTEKSI INSTAN 2: Validasi Ketersediaan Kunci di Redis (Skenario 1 Menit)
                // Jika TTL 1 menit di Redis sudah habis, maka data booking di Redis akan null
                for (OrderDetail detail : order.getOrderDetails()) {
                        String userBookingKey = "booking:user:" + detail.getProduct().getId() + ":" + currentUsername;
                        String userBookingStr = redisTemplate.opsForValue().get(userBookingKey);

                        if (userBookingStr == null) {
                                order.setStatus("CANCELLED");
                                orderRepository.save(order);
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Maaf, slot booking produk " + detail.getProduct().getName()
                                                                + " sudah hangus di Redis karena kamu terlalu lama.");
                        }
                }

                // 2. Kumpulkan semua Product ID dari detail order untuk Batch Fetch MySQL
                List<String> productIds = order.getOrderDetails().stream()
                                .map(detail -> detail.getProduct().getId())
                                .collect(Collectors.toList());

                // BATCH FETCH MYSQL (1x Query): Ambil data produk asli dari DB untuk dipotong
                // stoknya
                List<Product> productsFromDb = productRepository.findAllById(productIds);
                Map<String, Product> productMap = productsFromDb.stream()
                                .collect(Collectors.toMap(Product::getId, p -> p));

                List<OrderDetailResponseDto> detailResponses = new ArrayList<>();

                // 3. EKSEKUSI PEMOTONGAN STOK ASLI DI MYSQL & BERSIHKAN REDIS
                for (OrderDetail detail : order.getOrderDetails()) {
                        Product product = productMap.get(detail.getProduct().getId());

                        // Jaga-jaga jika stok fisik di DB tiba-tiba kurang (Hard Validation)
                        if (product.getStock() < detail.getQuantity()) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Maaf, stok fisik produk " + product.getName() + " sudah habis.");
                        }

                        // 📉 A. Potong Stok Permanen di MySQL
                        product.setStock(product.getStock() - detail.getQuantity());
                        productRepository.save(product); // Simpan perubahan stok ke MySQL

                        // 🧼 B. Bersihkan Kunci di Redis karena Barang Sudah Resmi Terbeli
                        String userBookingKey = "booking:user:" + product.getId() + ":" + currentUsername;
                        String globalBookingKey = "booking:total:product:" + product.getId();

                        // Hapus draf pribadi user di Redis
                        redisTemplate.delete(userBookingKey);

                        // Kurangi total booking global di Redis agar kuota kembali akurat untuk user
                        // lain
                        String globalTotalStr = redisTemplate.opsForValue().get(globalBookingKey);
                        if (globalTotalStr != null) {
                                int newGlobal = Math.max(0, Integer.parseInt(globalTotalStr) - detail.getQuantity());
                                redisTemplate.opsForValue().set(globalBookingKey, String.valueOf(newGlobal),
                                                Duration.ofMinutes(1));
                        }

                        detailResponses.add(OrderDetailResponseDto.builder()
                                        .productId(product.getId())
                                        .productName(product.getName())
                                        .quantity(detail.getQuantity())
                                        .price(detail.getPrice())
                                        .build());
                }

                // 4. Ubah Status Order Menjadi PAID (Lunas)
                order.setStatus("PAID");
                orderRepository.save(order);

                // 5. Kembalikan Response DTO
                return OrderResponseDto.builder()
                                .orderId(order.getId())
                                .username(order.getUsername())
                                .orderDate(order.getOrderDate())
                                .totalAmount(order.getTotalAmount())
                                .status(order.getStatus())
                                .items(detailResponses)
                                .build();
        }
}