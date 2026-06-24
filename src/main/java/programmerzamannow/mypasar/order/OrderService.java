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
import programmerzamannow.mypasar.audit.entity.AuditLog;
import programmerzamannow.mypasar.order.dto.*;
import programmerzamannow.mypasar.order.entity.Order;
import programmerzamannow.mypasar.order.entity.OrderDetail;
import programmerzamannow.mypasar.product.ProductRepository;
import programmerzamannow.mypasar.product.entity.Product;

import programmerzamannow.mypasar.shared.validation.ValidationService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

        @Transactional
        public OrderResponseDto createOrder(String currentUsername, OrderRequestDto request) {
                validationService.validate(request);

                // 1. KUMPULKAN SEMUA PRODUCT ID
                List<String> productIds = request.getItems().stream()
                                .map(OrderItemDto::getProductId)
                                .collect(Collectors.toList());

                // 2. QUERY KE MYSQL HANYA 1 KALI (BATCH FETCH)
                List<Product> productsFromDb = productRepository.findAllById(productIds);
                java.util.Map<String, Product> productMap = productsFromDb.stream()
                                .collect(Collectors.toMap(Product::getId, product -> product));

                // Validasi keberadaan produk di DB
                for (String id : productIds) {
                        if (!productMap.containsKey(id)) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Product tidak ditemukan dengan ID: " + id);
                        }
                }

                // 3. ✨ SOLUSI SAKTI: AMBIL DATA BOOKING REDIS SECARA SPESIFIK (TANPA KEYS*)
                // Kita buat daftar key booking spesifik milik USER LAIN untuk produk-produk ini
                // Karena kita tidak bisa menebak username user lain, kita akan mengubah
                // strategi:
                // Kita simpan total booking per produk di satu KEY AGREGAT khusus:
                // "booking:total:product:{productId}"

                List<String> totalBookingKeys = productIds.stream()
                                .map(id -> "booking:total:product:" + id)
                                .collect(Collectors.toList());

                // Ambil semua total booking sekaligus dari Redis (Batch Get)
                List<String> totalAllocatedInRedis = redisTemplate.opsForValue().multiGet(totalBookingKeys);

                // Kita juga ambil booking spesifik milik user ini sendiri untuk mendeteksi
                // akumulasi
                List<String> userSpecificKeys = productIds.stream()
                                .map(id -> "booking:user:" + id + ":" + currentUsername)
                                .collect(Collectors.toList());
                List<String> userOldBookings = redisTemplate.opsForValue().multiGet(userSpecificKeys);

                // 4. MULAI PROSES ORDER
                long waktuSekarang = System.currentTimeMillis();
                long duaMenitDalamMilidetik = 2 * 60 * 1000; // 120,000 ms
                Order order = Order.builder()
                                .id(UUID.randomUUID().toString())
                                .username(currentUsername)
                                .orderDate(System.currentTimeMillis())
                                .status("PENDING")
                                .totalAmount(BigDecimal.ZERO)
                                .expiredAt(waktuSekarang + duaMenitDalamMilidetik)
                                .build();

                List<OrderDetail> orderDetails = new ArrayList<>();
                List<OrderDetailResponseDto> detailResponses = new ArrayList<>();
                BigDecimal totalAmount = BigDecimal.ZERO;

                // 5. LOOPING VALIDASI DENGAN DATA YANG SUDAH TERISOLASI
                for (int i = 0; i < request.getItems().size(); i++) {
                        OrderItemDto itemDto = request.getItems().get(i);
                        Product product = productMap.get(itemDto.getProductId());

                        // Ambil data booking global untuk produk ini dari hasil batch get tadi
                        String totalBookingStr = totalAllocatedInRedis != null ? totalAllocatedInRedis.get(i) : null;
                        int globalBookedInRedis = totalBookingStr != null ? Integer.parseInt(totalBookingStr) : 0;

                        // Ambil data booking lama khusus user ini dari hasil batch get
                        String userOldBookingStr = userOldBookings != null ? userOldBookings.get(i) : null;
                        int currentUserOldBooking = userOldBookingStr != null ? Integer.parseInt(userOldBookingStr) : 0;

                        // Hitung berapa total booking milik ORANG LAIN
                        int bookedByOthers = globalBookedInRedis - currentUserOldBooking;

                        // Sisa stok aktual yang boleh diperebutkan oleh user ini
                        int availableStockForThisUser = product.getStock() - bookedByOthers;

                        // Total akumulasi permintaan baru + lama dari user ini
                        int totalRequestedByThisUser = itemDto.getQuantity() + currentUserOldBooking;

                        // VALIDASI AKURAT KITA (Terisolasi per item, tidak akan tertukar!)
                        if (availableStockForThisUser < totalRequestedByThisUser) {
                                int maxCanAdd = availableStockForThisUser - currentUserOldBooking;
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Gagal menambah ke keranjang untuk " + product.getName() +
                                                                ". Sisa kuota yang bisa kamu tambah maksimal tinggal "
                                                                + maxCanAdd + " item lagi.");
                        }

                        // 6. UPDATE DATA DI REDIS SECARA AKURAT
                        // Set booking pribadi user
                        String userBookingKey = "booking:user:" + product.getId() + ":" + currentUsername;
                        redisTemplate.opsForValue().set(userBookingKey, String.valueOf(totalRequestedByThisUser),
                                        Duration.ofMinutes(1));

                        // Set/Increment total booking global untuk produk ini
                        String globalBookingKey = "booking:total:product:" + product.getId();
                        int newGlobalTotal = globalBookedInRedis + itemDto.getQuantity();
                        redisTemplate.opsForValue().set(globalBookingKey, String.valueOf(newGlobalTotal),
                                        Duration.ofMinutes(1));

                        // Kalkulasi harga
                        BigDecimal itemPrice = product.getPrice();
                        BigDecimal subTotal = itemPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                        totalAmount = totalAmount.add(subTotal);

                        OrderDetail detail = OrderDetail.builder()
                                        .id(UUID.randomUUID().toString())
                                        .order(order)
                                        .product(product)
                                        .quantity(itemDto.getQuantity())
                                        .price(itemPrice)
                                        .build();
                        orderDetails.add(detail);

                        detailResponses.add(OrderDetailResponseDto.builder()
                                        .productId(product.getId())
                                        .productName(product.getName())
                                        .quantity(itemDto.getQuantity())
                                        .price(itemPrice)
                                        .build());
                }

                // 7. SIMPAN KE MYSQL
                order.setTotalAmount(totalAmount);
                order.setOrderDetails(orderDetails);
                orderRepository.save(order);

                // Catat Audit Trail
                AuditLog auditLog = AuditLog.builder()
                                .id(UUID.randomUUID().toString())
                                .username(currentUsername)
                                .action("ADD_TO_CART_ISOLATED")
                                .entityName("Order")
                                .entityId(order.getId())
                                .createdAt(System.currentTimeMillis())
                                .build();
                auditLogRepository.save(auditLog);

                return OrderResponseDto.builder()
                                .orderId(order.getId())
                                .username(order.getUsername())
                                .orderDate(order.getOrderDate())
                                .totalAmount(order.getTotalAmount())
                                .status(order.getStatus())
                                .items(detailResponses)
                                .build();
        }

        @Scheduled(fixedRate = 6000) // Berjalan otomatis setiap 2 menit
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

                // 4. BATCH GET REDIS: Ambil total booking global dan booking user saat ini
                // sekaligus
                List<String> globalBookingKeys = productIds.stream().map(id -> "booking:total:product:" + id)
                                .collect(Collectors.toList());
                List<String> userBookingKeys = productIds.stream()
                                .map(id -> "booking:user:" + id + ":" + currentUsername).collect(Collectors.toList());

                List<String> totalAllocatedInRedis = redisTemplate.opsForValue().multiGet(globalBookingKeys);
                List<String> userOldBookings = redisTemplate.opsForValue().multiGet(userBookingKeys);

                // Ubah detail order MySQL menjadi Map agar mudah dicocokkan berdasarkan ID
                // produk
                java.util.Map<String, OrderDetail> detailMap = order.getOrderDetails().stream()
                                .collect(Collectors.toMap(detail -> detail.getProduct().getId(), detail -> detail));

                // 5. LOOPING VALIDASI DAN HITUNG DELTA SECARA MASSAL
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

                        // Ambil data Redis dari hasil Batch Get tadi
                        String totalBookingStr = totalAllocatedInRedis != null ? totalAllocatedInRedis.get(i) : null;
                        int globalBookedInRedis = totalBookingStr != null ? Integer.parseInt(totalBookingStr) : 0;

                        String userOldBookingStr = userOldBookings != null ? userOldBookings.get(i) : null;
                        int currentUserOldBooking = userOldBookingStr != null ? Integer.parseInt(userOldBookingStr) : 0;

                        // VALIDASI JIKA TOMBOL `+` DITEKAN (Delta Positif)
                        if (delta > 0) {
                                int bookedByOthers = globalBookedInRedis - currentUserOldBooking;
                                int availableStockForThisUser = product.getStock() - bookedByOthers;

                                if (availableStockForThisUser < newQuantity) {
                                        int maxCanAdd = availableStockForThisUser - currentUserOldBooking;
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "Stok " + product.getName()
                                                                        + " tidak mencukupi. Maksimal tambah: "
                                                                        + maxCanAdd);
                                }
                        }

                        // 6. UPDATE DATA REDIS (1 Menit sesuai skenario eksperimen)
                        String userBookingKey = "booking:user:" + product.getId() + ":" + currentUsername;
                        String globalBookingKey = "booking:total:product:" + product.getId();

                        redisTemplate.opsForValue().set(userBookingKey, String.valueOf(newQuantity),
                                        Duration.ofMinutes(1));
                        redisTemplate.opsForValue().set(globalBookingKey, String.valueOf(globalBookedInRedis + delta),
                                        Duration.ofMinutes(1));

                        // 7. UPDATE DETAIL QUANTITY DI MEMORI JAVA
                        targetDetail.setQuantity(newQuantity);
                }

                // 8. HITUNG ULANG TOTAL HARGA DAN SIMPAN SEKALIGUS KE MYSQL
                BigDecimal totalAmount = order.getOrderDetails().stream()
                                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                order.setTotalAmount(totalAmount);

                orderRepository.save(order); // 🔥 1 Kali Simpan untuk semua perubahan produk!

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
}