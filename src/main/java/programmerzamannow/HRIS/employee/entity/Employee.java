package programmerzamannow.HRIS.employee.entity;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.HRIS.attedance.entity.Attendance;
import programmerzamannow.HRIS.auth.entity.User;
import programmerzamannow.HRIS.departement.entity.Department;
import programmerzamannow.HRIS.payroll.entity.Payroll;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    // Employee yang memegang kendali penuh untuk men-save User secara otomatis
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "username", referencedColumnName = "username", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "department_id", referencedColumnName = "id")
    private Department department;

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

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<Attendance> attendances;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<Payroll> payrolls;
}