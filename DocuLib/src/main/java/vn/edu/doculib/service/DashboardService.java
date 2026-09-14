package vn.edu.doculib.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.dto.DashboardStats;
import vn.edu.doculib.model.AcquisitionStatus;
import vn.edu.doculib.model.MaterialStatus;
import vn.edu.doculib.model.ResourceMaterial;
import vn.edu.doculib.model.AcquisitionRequest;
import vn.edu.doculib.repository.AcquisitionRequestRepository;
import vn.edu.doculib.repository.ResourceMaterialRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardService {

    private final ResourceMaterialRepository materialRepository;
    private final AcquisitionRequestRepository acquisitionRepository;

    public DashboardService(ResourceMaterialRepository materialRepository,
                            AcquisitionRequestRepository acquisitionRepository) {
        this.materialRepository = materialRepository;
        this.acquisitionRepository = acquisitionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        long totalTitles = materialRepository.countByTrashedFalse();
        long available = materialRepository.countByStatusAndTrashedFalse(MaterialStatus.AVAILABLE);
        long processing = materialRepository.countByStatusAndTrashedFalse(MaterialStatus.PROCESSING);
        long limited = materialRepository.countByStatusAndTrashedFalse(MaterialStatus.LIMITED);
        long archived = materialRepository.countByStatusAndTrashedFalse(MaterialStatus.ARCHIVED);
        long proposed = acquisitionRepository.countByStatus(AcquisitionStatus.PROPOSED);
        long reviewing = acquisitionRepository.countByStatus(AcquisitionStatus.REVIEWING);
        long approved = acquisitionRepository.countByStatus(AcquisitionStatus.APPROVED);
        long ordered = acquisitionRepository.countByStatus(AcquisitionStatus.ORDERED);
        long received = acquisitionRepository.countByStatus(AcquisitionStatus.RECEIVED);
        long pending = proposed + reviewing;
        BigDecimal budget = acquisitionRepository.sumBudgetByStatuses(
                List.of(AcquisitionStatus.APPROVED, AcquisitionStatus.ORDERED));
        return new DashboardStats(
                totalTitles,
                materialRepository.sumTotalQuantity(),
                available,
                processing,
                limited,
                archived,
                pending,
                proposed,
                reviewing,
                approved,
                ordered,
                received,
                budget == null ? BigDecimal.ZERO : budget,
                percentage(available, totalTitles),
                percentage(processing, totalTitles),
                percentage(limited, totalTitles),
                percentage(archived, totalTitles),
                percentage(materialRepository.countCompleteRecords(), totalTitles)
        );
    }

    @Transactional(readOnly = true)
    public List<ResourceMaterial> getRecentMaterials() {
        return materialRepository.findTop5ByTrashedFalseOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AcquisitionRequest> getRecentAcquisitions() {
        return acquisitionRepository.findTop5ByOrderByRequestedAtDesc();
    }

    private int percentage(long part, long total) {
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }
}
