package programmerzamannow.HRIS.departement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import programmerzamannow.HRIS.departement.entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
}