package vn.edu.doculib.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.model.MaterialAuditAction;
import vn.edu.doculib.model.MaterialAuditLog;
import vn.edu.doculib.model.ResourceMaterial;
import vn.edu.doculib.repository.MaterialAuditLogRepository;

import java.util.List;

@Service
public class MaterialAuditService {

    private final MaterialAuditLogRepository auditRepository;

    public MaterialAuditService(MaterialAuditLogRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void record(ResourceMaterial material, MaterialAuditAction action, String actorUsername, String details) {
        MaterialAuditLog log = new MaterialAuditLog();
        log.setMaterialId(material.getId());
        log.setInventoryCode(material.getInventoryCode());
        log.setMaterialTitle(material.getTitle());
        log.setAction(action);
        log.setActorUsername(AuditTextSanitizer.actor(actorUsername));
        log.setDetails(AuditTextSanitizer.sanitize(details));
        auditRepository.save(log);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public List<MaterialAuditLog> findForMaterial(Long materialId) {
        return auditRepository.findAllByMaterialIdOrderByOccurredAtDesc(materialId);
    }
}
