package programmerzamannow.mypasar.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import programmerzamannow.mypasar.audit.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
}