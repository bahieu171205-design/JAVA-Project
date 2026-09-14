package vn.edu.doculib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.doculib.model.MaterialShare;

import java.util.List;
import java.util.Optional;

public interface MaterialShareRepository extends JpaRepository<MaterialShare, Long> {

    List<MaterialShare> findAllBySharedWithUsernameIgnoreCaseAndMaterialTrashedFalseOrderBySharedAtDesc(
            String username);

    List<MaterialShare> findAllByMaterialIdOrderBySharedAtDesc(Long materialId);

    Optional<MaterialShare> findByMaterialIdAndSharedWithId(Long materialId, Long sharedWithId);

    void deleteAllByMaterialId(Long materialId);
}
