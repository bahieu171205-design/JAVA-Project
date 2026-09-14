package vn.edu.doculib.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.doculib.model.MaterialStatus;
import vn.edu.doculib.model.MaterialType;
import vn.edu.doculib.model.ResourceMaterial;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ResourceMaterialRepository extends JpaRepository<ResourceMaterial, Long> {

    @Query(value = """
            select distinct m from ResourceMaterial m
            left join m.authors a
            where m.trashed = false
              and (:query is null or :query = ''
                   or lower(m.title) like lower(concat('%', :query, '%'))
                   or lower(m.inventoryCode) like lower(concat('%', :query, '%'))
                   or lower(coalesce(m.isbnIssn, '')) like lower(concat('%', :query, '%'))
                   or lower(a.name) like lower(concat('%', :query, '%')))
              and (:type is null or m.materialType = :type)
              and (:status is null or m.status = :status)
              and (:categoryId is null or m.category.id = :categoryId)
              and (:authorId is null or a.id = :authorId)
              and (:updatedFrom is null or m.updatedAt >= :updatedFrom)
            """,
            countQuery = """
            select count(distinct m.id) from ResourceMaterial m
            left join m.authors a
            where m.trashed = false
              and (:query is null or :query = ''
                   or lower(m.title) like lower(concat('%', :query, '%'))
                   or lower(m.inventoryCode) like lower(concat('%', :query, '%'))
                   or lower(coalesce(m.isbnIssn, '')) like lower(concat('%', :query, '%'))
                   or lower(a.name) like lower(concat('%', :query, '%')))
              and (:type is null or m.materialType = :type)
              and (:status is null or m.status = :status)
              and (:categoryId is null or m.category.id = :categoryId)
              and (:authorId is null or a.id = :authorId)
              and (:updatedFrom is null or m.updatedAt >= :updatedFrom)
            """)
    Page<ResourceMaterial> search(
            @Param("query") String query,
            @Param("type") MaterialType type,
            @Param("status") MaterialStatus status,
            @Param("categoryId") Long categoryId,
            @Param("authorId") Long authorId,
            @Param("updatedFrom") LocalDateTime updatedFrom,
            Pageable pageable);

    boolean existsByInventoryCodeIgnoreCase(String inventoryCode);

    boolean existsByInventoryCodeIgnoreCaseAndIdNot(String inventoryCode, Long id);

    Optional<ResourceMaterial> findByIdAndTrashedFalse(Long id);

    Optional<ResourceMaterial> findByIdAndTrashedTrue(Long id);

    Optional<ResourceMaterial> findTopByInventoryCodeStartingWithOrderByInventoryCodeDesc(String prefix);

    List<ResourceMaterial> findAllByTrashedTrueOrderByTrashedAtDesc();

    long countByTrashedFalse();

    long countByStatusAndTrashedFalse(MaterialStatus status);

    @Query("select coalesce(sum(m.quantity), 0) from ResourceMaterial m where m.trashed = false")
    Long sumTotalQuantity();

    @Query("""
            select count(m) from ResourceMaterial m
            where m.trashed = false
              and m.category is not null
              and m.publisher is not null
              and m.isbnIssn is not null and m.isbnIssn <> ''
              and m.callNumber is not null and m.callNumber <> ''
              and size(m.authors) > 0
            """)
    long countCompleteRecords();

    List<ResourceMaterial> findTop5ByTrashedFalseOrderByUpdatedAtDesc();
}
