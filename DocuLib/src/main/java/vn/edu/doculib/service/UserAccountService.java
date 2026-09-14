package vn.edu.doculib.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.doculib.dto.UserAccountForm;
import vn.edu.doculib.exception.ResourceNotFoundException;
import vn.edu.doculib.exception.UserAccountValidationException;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.model.UserRole;
import vn.edu.doculib.repository.UserAccountRepository;

import java.util.List;
import java.util.Locale;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public UserAccountService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
                              ApplicationEventPublisher eventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findAll() {
        return userAccountRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public UserAccount getById(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản #" + id));
    }

    @Transactional(readOnly = true)
    public UserAccountForm getForm(Long id) {
        return UserAccountForm.fromEntity(getById(id));
    }

    @Transactional
    public UserAccount save(UserAccountForm form, String currentUsername) {
        boolean creating = form.getId() == null;
        UserAccount account = creating ? new UserAccount() : getById(form.getId());
        String username = normalizeUsername(form.getUsername());
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        UserRole previousRole = account.getRole();
        boolean previouslyEnabled = account.isEnabled();
        boolean passwordChanged = StringUtils.hasText(form.getPassword());

        if (creating && userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new UserAccountValidationException("username", "Tên đăng nhập đã được sử dụng");
        }
        if (creating ? userAccountRepository.existsByEmailIgnoreCase(email)
                : userAccountRepository.existsByEmailIgnoreCaseAndIdNot(email, account.getId())) {
            throw new UserAccountValidationException("email", "Email đã được sử dụng");
        }
        validatePassword(form, creating);
        protectAdministrativeAccount(account, form.getRole(), form.isEnabled(), currentUsername);

        if (creating) {
            account.setUsername(username);
        } else {
            form.setUsername(account.getUsername());
        }
        account.setFullName(form.getFullName().trim());
        account.setEmail(email);
        account.setRole(form.getRole());
        account.setEnabled(form.isEnabled());
        if (StringUtils.hasText(form.getPassword())) {
            account.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }
        UserAccount saved = userAccountRepository.save(account);
        boolean existingSecurityStateChanged = !creating
                && (previousRole != saved.getRole()
                || previouslyEnabled != saved.isEnabled()
                || passwordChanged);
        if (existingSecurityStateChanged) {
            publishSessionInvalidation(saved.getUsername());
        }
        return saved;
    }

    @Transactional
    public boolean toggleEnabled(Long id, String currentUsername) {
        UserAccount account = getById(id);
        boolean newState = !account.isEnabled();
        protectAdministrativeAccount(account, account.getRole(), newState, currentUsername);
        account.setEnabled(newState);
        if (!newState) {
            publishSessionInvalidation(account.getUsername());
        }
        return newState;
    }

    private void publishSessionInvalidation(String username) {
        eventPublisher.publishEvent(new UserAccountSecurityChangedEvent(username));
    }

    private void validatePassword(UserAccountForm form, boolean required) {
        if (!StringUtils.hasText(form.getPassword())) {
            if (required) {
                throw new UserAccountValidationException("password", "Mật khẩu không được để trống");
            }
            return;
        }
        if (form.getPassword().length() < 8) {
            throw new UserAccountValidationException("password", "Mật khẩu phải có ít nhất 8 ký tự");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new UserAccountValidationException("confirmPassword", "Mật khẩu xác nhận không khớp");
        }
    }

    private void protectAdministrativeAccount(UserAccount account, UserRole newRole,
                                                boolean newEnabled, String currentUsername) {
        if (account.getId() == null) {
            return;
        }
        if (account.getUsername().equalsIgnoreCase(currentUsername) && !newEnabled) {
            throw new UserAccountValidationException("enabled", "Bạn không thể vô hiệu hóa tài khoản đang đăng nhập");
        }
        if (account.getUsername().equalsIgnoreCase(currentUsername) && account.getRole() != newRole) {
            throw new UserAccountValidationException("role", "Bạn không thể thay đổi vai trò của phiên đang đăng nhập");
        }
        boolean removesActiveAdmin = account.getRole() == UserRole.ADMIN && account.isEnabled()
                && (newRole != UserRole.ADMIN || !newEnabled);
        if (removesActiveAdmin && userAccountRepository.countByRoleAndEnabledTrue(UserRole.ADMIN) <= 1) {
            throw new UserAccountValidationException("role", "Hệ thống phải còn ít nhất một quản trị viên hoạt động");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
