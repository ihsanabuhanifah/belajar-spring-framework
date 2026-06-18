package programmerzamannow.HRIS.departement.entity;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.HRIS.employee.entity.Employee;

import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Relasi ke Employee (Satu departemen bisa menampung banyak karyawan)
    @OneToMany(mappedBy = "department")
    private List<Employee> employees;
}