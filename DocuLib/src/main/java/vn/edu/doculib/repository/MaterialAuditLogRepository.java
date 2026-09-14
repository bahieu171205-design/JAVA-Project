package vn.edu.doculib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.doculib.model.MaterialAuditLog;

import java.util.List;

public interface MaterialAuditLogRepository extends JpaRepository<MaterialAuditLog, Long> {

    List<MaterialAuditLog> findAllByMaterialIdOrderByOccurredAtDesc(Long materialId);
}
