package vn.edu.doculib.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.model.UserRole;
import vn.edu.doculib.repository.UserAccountRepository;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.security.seed-default-users", havingValue = "true", matchIfMissing = true)
public class DefaultUserInitializer implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String librarianPassword;
    private final String viewerPassword;

    public DefaultUserInitializer(UserAccountRepository userAccountRepository,
                                  PasswordEncoder passwordEncoder,
                                  @Value("${app.security.admin-password:Admin@123}") String adminPassword,
                                  @Value("${app.security.librarian-password:BienMuc@123}") String librarianPassword,
                                  @Value("${app.security.viewer-password:BanDoc@123}") String viewerPassword) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.librarianPassword = librarianPassword;
        this.viewerPassword = viewerPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("admin", "Quản trị hệ thống", "admin@doculib.local", UserRole.ADMIN, adminPassword);
        createIfMissing("librarian", "Cán bộ biên mục", "librarian@doculib.local", UserRole.LIBRARIAN,
                librarianPassword);
        createIfMissing("viewer", "Người dùng tra cứu", "viewer@doculib.local", UserRole.VIEWER, viewerPassword);
    }

    private void createIfMissing(String username, String fullName, String email, UserRole role, String password) {
        if (userAccountRepository.existsByUsernameIgnoreCase(username)
                || userAccountRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setFullName(fullName);
        account.setEmail(email);
        account.setRole(role);
        account.setEnabled(true);
        account.setPasswordHash(passwordEncoder.encode(password));
        userAccountRepository.save(account);
    }
}
