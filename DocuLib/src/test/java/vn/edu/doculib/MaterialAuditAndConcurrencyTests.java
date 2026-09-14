package vn.edu.doculib;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.doculib.dto.MaterialForm;
import vn.edu.doculib.exception.ConcurrentUpdateException;
import vn.edu.doculib.model.MaterialAuditAction;
import vn.edu.doculib.model.MaterialStatus;
import vn.edu.doculib.model.MaterialType;
import vn.edu.doculib.model.ResourceMaterial;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.model.UserRole;
import vn.edu.doculib.repository.MaterialAuditLogRepository;
import vn.edu.doculib.repository.UserAccountRepository;
import vn.edu.doculib.service.MaterialInteractionService;
import vn.edu.doculib.service.MaterialService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MaterialAuditAndConcurrencyTests {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialInteractionService interactionService;

    @Autowired
    private MaterialAuditLogRepository auditRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createEditShareRevokeTrashAndRestoreAreAudited() {
        UserAccount editor = account("audit.editor", UserRole.LIBRARIAN);
        UserAccount viewer = account("audit.viewer", UserRole.VIEWER);
        MaterialForm form = newMaterial("TL-AUDIT-001");

        ResourceMaterial material = materialService.save(form, editor.getUsername());
        MaterialForm update = MaterialForm.fromEntity(material);
        update.setTitle("Tài liệu đã cập nhật");
        materialService.save(update, editor.getUsername());
        interactionService.share(material.getId(), viewer.getId(), editor.getUsername());
        interactionService.revokeShare(material.getId(), viewer.getId(), editor.getUsername());
        materialService.moveToTrash(material.getId(), editor.getUsername());
        materialService.restore(material.getId(), editor.getUsername());

        assertThat(auditRepository.findAllByMaterialIdOrderByOccurredAtDesc(material.getId()))
                .extracting(log -> log.getAction())
                .containsExactlyInAnyOrder(
                        MaterialAuditAction.CREATED,
                        MaterialAuditAction.UPDATED,
                        MaterialAuditAction.SHARED,
                        MaterialAuditAction.SHARE_REVOKED,
                        MaterialAuditAction.MOVED_TO_TRASH,
                        MaterialAuditAction.RESTORED);
    }

    @Test
    void staleMaterialVersionIsRejectedInsteadOfOverwritingNewerData() {
        UserAccount editor = account("conflict.editor", UserRole.LIBRARIAN);
        ResourceMaterial material = materialService.save(newMaterial("TL-CONFLICT-001"), editor.getUsername());
        MaterialForm firstEditor = MaterialForm.fromEntity(material);
        MaterialForm staleEditor = MaterialForm.fromEntity(material);

        firstEditor.setTitle("Thay đổi từ trình duyệt thứ nhất");
        materialService.save(firstEditor, editor.getUsername());
        staleEditor.setTitle("Thay đổi cũ từ trình duyệt thứ hai");

        assertThatThrownBy(() -> materialService.save(staleEditor, editor.getUsername()))
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("người khác cập nhật");
        assertThat(materialService.getById(material.getId()).getTitle())
                .isEqualTo("Thay đổi từ trình duyệt thứ nhất");
    }

    private MaterialForm newMaterial(String code) {
        MaterialForm form = new MaterialForm();
        form.setInventoryCode(code);
        form.setTitle("Tài liệu kiểm thử nhật ký");
        form.setMaterialType(MaterialType.BOOK);
        form.setStatus(MaterialStatus.PROCESSING);
        form.setQuantity(1);
        return form;
    }

    private UserAccount account(String username, UserRole role) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setFullName("Kiểm thử " + username);
        account.setEmail(username + "@doculib.test");
        account.setPasswordHash(passwordEncoder.encode("KiemThu@123"));
        account.setRole(role);
        account.setEnabled(true);
        return userRepository.saveAndFlush(account);
    }
}
