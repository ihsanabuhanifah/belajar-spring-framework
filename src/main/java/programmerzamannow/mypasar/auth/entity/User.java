package programmerzamannow.mypasar.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.mypasar.order.entity.Order;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password", length = 200, nullable = false)
    private String password;

    @Column(name = "role", length = 50, nullable = false)
    private String role; // ADMIN, HRD, EMPLOYEE, CUSTOMER

    @Column(name = "token", length = 100)
    private String token;

    @Column(name = "token_expired_at")
    private Long tokenExpiredAt;

    // Relasi 1-to-1 ke detail profil identity (Employee)
    // @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    // private Employee employee;

    // Relasi ke transaksi toko online (Orders)
    // @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    // private List<Order> orders;
}