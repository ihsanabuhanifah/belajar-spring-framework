package programmerzamannow.mypasar.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import programmerzamannow.mypasar.employee.entity.Employee;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    // Fungsi untuk mencari profil karyawan berdasarkan akun loginnya
    Optional<Employee> findByUsername(String username);
}
