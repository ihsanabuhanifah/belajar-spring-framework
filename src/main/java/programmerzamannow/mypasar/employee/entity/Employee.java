package programmerzamannow.mypasar.employee.entity;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.mypasar.auth.entity.User;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    // Mengunci ke akun utamanya

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "job_title", length = 100, nullable = false)
    private String jobTitle;

    @Column(name = "joined_at", nullable = false)
    private Long joinedAt;
}