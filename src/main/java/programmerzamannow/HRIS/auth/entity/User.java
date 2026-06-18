package programmerzamannow.HRIS.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.HRIS.employee.entity.Employee;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password", length = 200, nullable = false)
    private String password;

    @Column(name = "role", length = 50, nullable = false)
    private String role;

    @Column(name = "token", length = 100, unique = true)
    private String token;

    @Column(name = "token_expired_at")
    private Long tokenExpiredAt;

    // Cukup mappedBy saja tanpa cascade=ALL, biar tidak bentrok dua arah
    @OneToOne(mappedBy = "user")
    private Employee employee;
}