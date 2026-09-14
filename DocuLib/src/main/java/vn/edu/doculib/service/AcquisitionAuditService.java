package vn.edu.doculib.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.model.AcquisitionRequest;
import vn.edu.doculib.model.AcquisitionStatus;
import vn.edu.doculib.model.AcquisitionStatusHistory;
import vn.edu.doculib.repository.AcquisitionStatusHistoryRepository;

import java.util.List;

@Service
public class AcquisitionAuditService {

    private final AcquisitionStatusHistoryRepository historyRepository;

    public AcquisitionAuditService(AcquisitionStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void record(AcquisitionRequest request, AcquisitionStatus fromStatus, AcquisitionStatus toStatus,
                       String actorUsername, String note) {
        AcquisitionStatusHistory history = new AcquisitionStatusHistory();
        history.setRequestId(request.getId());
        history.setRequestCode(request.getRequestCode());
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setActorUsername(AuditTextSanitizer.actor(actorUsername));
        history.setNote(AuditTextSanitizer.sanitize(note));
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public List<AcquisitionStatusHistory> findForRequest(Long requestId) {
        return historyRepository.findAllByRequestIdOrderByChangedAtDesc(requestId);
    }
}
