package vn.edu.doculib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.doculib.model.AcquisitionStatusHistory;

import java.util.List;

public interface AcquisitionStatusHistoryRepository extends JpaRepository<AcquisitionStatusHistory, Long> {

    List<AcquisitionStatusHistory> findAllByRequestIdOrderByChangedAtDesc(Long requestId);
}
