package vn.edu.doculib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.model.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    List<UserAccount> findAllByOrderByFullNameAsc();

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByRoleAndEnabledTrue(UserRole role);
}
