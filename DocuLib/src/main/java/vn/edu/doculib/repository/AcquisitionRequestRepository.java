package vn.edu.doculib.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.doculib.model.AcquisitionRequest;
import vn.edu.doculib.model.AcquisitionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AcquisitionRequestRepository extends JpaRepository<AcquisitionRequest, Long> {

    @Query("""
            select r from AcquisitionRequest r
            where (:query is null or :query = ''
                   or lower(r.proposedTitle) like lower(concat('%', :query, '%'))
                   or lower(r.requestCode) like lower(concat('%', :query, '%'))
                   or lower(r.requester) like lower(concat('%', :query, '%')))
              and (:status is null or r.status = :status)
            """)
    Page<AcquisitionRequest> search(
            @Param("query") String query,
            @Param("status") AcquisitionStatus status,
            Pageable pageable);

    boolean existsByRequestCode(String requestCode);

    Optional<AcquisitionRequest> findTopByRequestCodeStartingWithOrderByRequestCodeDesc(String prefix);

    long countByStatus(AcquisitionStatus status);

    @Query("""
            select coalesce(sum(r.estimatedPrice * r.quantity), 0)
            from AcquisitionRequest r
            where r.status in :statuses
            """)
    BigDecimal sumBudgetByStatuses(@Param("statuses") List<AcquisitionStatus> statuses);

    List<AcquisitionRequest> findTop5ByOrderByRequestedAtDesc();
}
