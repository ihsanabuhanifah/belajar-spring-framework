package programmerzamannow.HRIS.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import programmerzamannow.HRIS.payroll.entity.Payroll;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, String> {
}