package programmerzamannow.mypasar.order.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "order_date", nullable = false)
    private Long orderDate;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", length = 50, nullable = false)
    private String status; // PENDING, PAID, CANCELLED

    // Pembungkus relasi ke anak-anaknya tetap dipertahankan
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails;
}