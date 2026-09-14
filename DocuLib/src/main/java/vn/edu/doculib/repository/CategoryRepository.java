package vn.edu.doculib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.doculib.model.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}
