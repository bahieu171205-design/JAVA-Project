package vn.edu.doculib.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.doculib.model.CodeSequence;

import java.util.Optional;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CodeSequence s where s.sequenceName = :name")
    Optional<CodeSequence> findForUpdate(@Param("name") String name);
}
