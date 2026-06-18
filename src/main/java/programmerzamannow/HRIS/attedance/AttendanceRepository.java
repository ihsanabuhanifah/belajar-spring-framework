package programmerzamannow.HRIS.attedance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import programmerzamannow.HRIS.attedance.entity.Attendance;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String> {
}