package programmerzamannow.HRIS.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import programmerzamannow.HRIS.employee.entity.Employee;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    // Fungsi untuk mencari profil karyawan berdasarkan akun loginnya
    Optional<Employee> findFirstByUser_Username(String username);
}