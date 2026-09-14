package vn.edu.doculib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.doculib.model.MaterialFavorite;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaterialFavoriteRepository extends JpaRepository<MaterialFavorite, Long> {

    Optional<MaterialFavorite> findByUserUsernameIgnoreCaseAndMaterialId(String username, Long materialId);

    boolean existsByUserUsernameIgnoreCaseAndMaterialId(String username, Long materialId);

    List<MaterialFavorite> findAllByUserUsernameIgnoreCaseAndMaterialTrashedFalseOrderByCreatedAtDesc(
            String username);

    List<MaterialFavorite> findAllByUserUsernameIgnoreCaseAndMaterialIdIn(
            String username, Collection<Long> materialIds);

    void deleteAllByMaterialId(Long materialId);
}
