package programmerzamannow.mypasar.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import programmerzamannow.mypasar.order.dto.*;
import programmerzamannow.mypasar.order.entity.Order;
import programmerzamannow.mypasar.order.entity.OrderDetail;
import programmerzamannow.mypasar.product.ProductRepository;
import programmerzamannow.mypasar.product.entity.Product;

import programmerzamannow.mypasar.shared.validation.ValidationService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private ValidationService validationService;

        @Transactional
        public OrderResponseDto createOrder(String currentUsername, OrderRequestDto request) {

                validationService.validate(request);

                Order order = Order.builder()
                                .id(UUID.randomUUID().toString())
                                .username(currentUsername)
                                .orderDate(System.currentTimeMillis())
                                .status("PENDING")
                                .totalAmount(BigDecimal.ZERO)
                                .build();

                List<OrderDetail> orderDetails = new ArrayList<>();
                List<OrderDetailResponseDto> detailResponses = new ArrayList<>();
                BigDecimal totalAmount = BigDecimal.ZERO;

                // 3. Looping untuk memproses satu per satu item di keranjang belanja
                for (OrderItemDto itemDto : request.getItems()) {
                        // Cek apakah produknya ada di MySQL
                        Product product = productRepository.findById(itemDto.getProductId())
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Product not found: " + itemDto.getProductId()));

                        // VALIDASI SAKTI: Cek apakah stok mencukupi
                        if (product.getStock() < itemDto.getQuantity()) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Insolvent stock for product: " + product.getName());
                        }

                        // POTONG STOK: Kurangi stok produk langsung di memori Java
                        product.setStock(product.getStock() - itemDto.getQuantity());
                        productRepository.save(product); // Update stok terbaru ke MySQL

                        // Hitung subtotal untuk item ini (harga * quantity)
                        BigDecimal itemPrice = product.getPrice();
                        BigDecimal subTotal = itemPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                        totalAmount = totalAmount.add(subTotal);

                        // Buat objek Detail Transaksi
                        OrderDetail detail = OrderDetail.builder()
                                        .id(UUID.randomUUID().toString())
                                        .order(order)
                                        .product(product)
                                        .quantity(itemDto.getQuantity())
                                        .price(itemPrice)
                                        .build();
                        orderDetails.add(detail);

                        // Masukkan ke manifes DTO response
                        detailResponses.add(OrderDetailResponseDto.builder()
                                        .productId(product.getId())
                                        .productName(product.getName())
                                        .quantity(itemDto.getQuantity())
                                        .price(itemPrice)
                                        .build());
                }

                // 4. Update total harga belanjaan yang sudah dihitung ke objek induk Order
                order.setTotalAmount(totalAmount);
                order.setOrderDetails(orderDetails);

                // 5. Simpan Transaksi Utuh ke Database
                orderRepository.save(order);

                // 6. Kembalikan struk digitalnya
                return OrderResponseDto.builder()
                                .orderId(order.getId())
                                .username(order.getUsername())
                                .orderDate(order.getOrderDate())
                                .totalAmount(order.getTotalAmount())
                                .status(order.getStatus())
                                .items(detailResponses)
                                .build();
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
}