package programmerzamannow.mypasar.order.entity;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.mypasar.auth.entity.User;

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

    // 🔗 Menembak ke User pembeli
    @ManyToOne
    @JoinColumn(name = "username", referencedColumnName = "username", nullable = false)
    private User user;

    @Column(name = "order_date", nullable = false)
    private Long orderDate;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "status", length = 50, nullable = false)
    private String status; // PENDING, PAID, SHIPPED

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;
}