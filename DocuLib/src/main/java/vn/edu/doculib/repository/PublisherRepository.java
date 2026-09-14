package vn.edu.doculib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.doculib.model.Publisher;

import java.util.List;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    List<Publisher> findAllByOrderByNameAsc();
}
