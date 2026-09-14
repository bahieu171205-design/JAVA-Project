package vn.edu.doculib;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.doculib.model.MaterialStatus;
import vn.edu.doculib.model.MaterialType;
import vn.edu.doculib.model.ResourceMaterial;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.model.UserRole;
import vn.edu.doculib.repository.AcquisitionRequestRepository;
import vn.edu.doculib.repository.MaterialFavoriteRepository;
import vn.edu.doculib.repository.MaterialShareRepository;
import vn.edu.doculib.repository.ResourceMaterialRepository;
import vn.edu.doculib.repository.UserAccountRepository;
import vn.edu.doculib.service.OnlineUserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocuLibApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceMaterialRepository materialRepository;

    @Autowired
    private AcquisitionRequestRepository acquisitionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private MaterialFavoriteRepository favoriteRepository;

    @Autowired
    private MaterialShareRepository shareRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OnlineUserService onlineUserService;

    @Test
    void contextLoads() {
        assertThat(materialRepository).isNotNull();
        assertThat(acquisitionRepository).isNotNull();
    }

    @Test
    void allMainPagesRenderSuccessfully() throws Exception {
        mockMvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
        mockMvc.perform(get("/materials").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/list"));
        mockMvc.perform(get("/materials/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/form"));
        mockMvc.perform(get("/acquisitions").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("acquisitions/list"));
        mockMvc.perform(get("/acquisitions/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("acquisitions/form"));
        mockMvc.perform(get("/reference-data").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("reference-data/index"));
        mockMvc.perform(get("/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"));
        mockMvc.perform(get("/materials/favorites").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/collection"));
        mockMvc.perform(get("/materials/shared").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/collection"));
        mockMvc.perform(get("/materials/trash").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/collection"));
    }

    @Test
    void canCreateMaterialAndAcquisitionThroughWebForms() throws Exception {
        mockMvc.perform(post("/materials/save")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .with(csrf())
                        .param("title", "Tài liệu kiểm thử")
                        .param("materialType", "BOOK")
                        .param("status", "PROCESSING")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/materials/*"));

        mockMvc.perform(post("/acquisitions/save")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .with(csrf())
                        .param("proposedTitle", "Nguồn tin kiểm thử")
                        .param("requester", "Tổ kiểm thử")
                        .param("reason", "Phục vụ kiểm tra nghiệp vụ tạo mới")
                        .param("quantity", "1")
                        .param("priority", "MEDIUM")
                        .param("status", "PROPOSED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/acquisitions*"));

        assertThat(materialRepository.count()).isEqualTo(1);
        assertThat(acquisitionRepository.count()).isEqualTo(1);

        Long materialId = materialRepository.findAll().getFirst().getId();
        mockMvc.perform(get("/materials/{id}", materialId)
                        .with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/detail"));
        mockMvc.perform(get("/materials")
                        .with(user("viewer").roles("VIEWER"))
                        .param("type", "BOOK")
                        .param("status", "PROCESSING")
                        .param("updatedFrom", "2000-01-01")
                        .param("sort", "title_asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/list"));
    }

    @Test
    void loginIsRequiredForApplicationPages() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void viewerCanReadButCannotEditOrManageUsers() throws Exception {
        mockMvc.perform(get("/materials").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/materials/new").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/users").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/materials/trash").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/materials/999/share")
                        .with(user("viewer").roles("VIEWER"))
                        .with(csrf())
                        .param("recipientId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void librarianCanEditButCannotDeleteOrManageReferenceData() throws Exception {
        mockMvc.perform(get("/materials/new").with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/materials/999/delete")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/reference-data/categories")
                        .with(user("librarian").roles("LIBRARIAN"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanCreateAnEncryptedDatabaseAccountAndUseItToLogin() throws Exception {
        mockMvc.perform(post("/users/save")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("username", "catalog.qa")
                        .param("fullName", "Kiểm thử tài khoản")
                        .param("email", "catalog.qa@doculib.local")
                        .param("password", "KiemThu@123")
                        .param("confirmPassword", "KiemThu@123")
                        .param("role", "LIBRARIAN")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/users/*/edit"));

        var account = userAccountRepository.findByUsernameIgnoreCase("catalog.qa").orElseThrow();
        assertThat(account.getPasswordHash()).isNotEqualTo("KiemThu@123");
        assertThat(passwordEncoder.matches("KiemThu@123", account.getPasswordHash())).isTrue();

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "catalog.qa")
                        .param("password", "KiemThu@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assertThat(onlineUserService.getOnlineUsernames(null)).contains("catalog.qa");
    }

    @Test
    void administratorCanChangeLoginPermissionWithTheEditSwitch() throws Exception {
        UserAccount account = createAccount("switch.qa", "Kiểm thử công tắc", UserRole.VIEWER);

        mockMvc.perform(post("/users/save")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("id", account.getId().toString())
                        .param("username", account.getUsername())
                        .param("fullName", account.getFullName())
                        .param("email", account.getEmail())
                        .param("role", account.getRole().name())
                        .param("_enabled", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + account.getId() + "/edit"));
        assertThat(userAccountRepository.findById(account.getId()).orElseThrow().isEnabled()).isFalse();

        mockMvc.perform(post("/users/save")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("id", account.getId().toString())
                        .param("username", account.getUsername())
                        .param("fullName", account.getFullName())
                        .param("email", account.getEmail())
                        .param("role", account.getRole().name())
                        .param("_enabled", "on")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + account.getId() + "/edit"));
        assertThat(userAccountRepository.findById(account.getId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void favoritesSharingAndTrashWorkWithRealDatabaseRelations() throws Exception {
        UserAccount admin = createAccount("feature.admin", "Quản trị tính năng", UserRole.ADMIN);
        UserAccount viewer = createAccount("feature.viewer", "Bạn đọc tính năng", UserRole.VIEWER);
        ResourceMaterial material = new ResourceMaterial();
        material.setInventoryCode("TL-FEATURE-001");
        material.setTitle("Tài liệu kiểm thử yêu thích và chia sẻ");
        material.setMaterialType(MaterialType.BOOK);
        material.setStatus(MaterialStatus.AVAILABLE);
        material.setQuantity(1);
        material = materialRepository.save(material);
        Long materialId = material.getId();

        mockMvc.perform(get("/materials/{id}", materialId)
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/detail"));

        mockMvc.perform(post("/materials/{id}/favorite", materialId)
                        .with(user(viewer.getUsername()).roles("VIEWER"))
                        .with(csrf())
                        .param("returnTo", "favorites"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/materials/favorites"));
        assertThat(favoriteRepository.existsByUserUsernameIgnoreCaseAndMaterialId(
                viewer.getUsername(), materialId)).isTrue();
        mockMvc.perform(get("/materials/favorites").with(user(viewer.getUsername()).roles("VIEWER")))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/collection"));

        mockMvc.perform(post("/materials/{id}/share", materialId)
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf())
                        .param("recipientId", viewer.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/materials/" + materialId));
        assertThat(shareRepository.findByMaterialIdAndSharedWithId(materialId, viewer.getId())).isPresent();
        mockMvc.perform(get("/materials/shared").with(user(viewer.getUsername()).roles("VIEWER")))
                .andExpect(status().isOk())
                .andExpect(view().name("materials/collection"));

        mockMvc.perform(post("/materials/{id}/delete", materialId)
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/materials"));
        assertThat(materialRepository.findByIdAndTrashedTrue(materialId)).isPresent();
        mockMvc.perform(get("/materials/{id}", materialId)
                        .with(user(viewer.getUsername()).roles("VIEWER")))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));

        mockMvc.perform(post("/materials/trash/{id}/restore", materialId)
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/materials/trash"));
        assertThat(materialRepository.findByIdAndTrashedFalse(materialId)).isPresent();
        assertThat(favoriteRepository.existsByUserUsernameIgnoreCaseAndMaterialId(
                viewer.getUsername(), materialId)).isTrue();
        assertThat(shareRepository.findByMaterialIdAndSharedWithId(materialId, viewer.getId())).isPresent();

        mockMvc.perform(post("/materials/{id}/delete", materialId)
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/materials/trash/{id}/delete", materialId)
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/materials/trash"));
        assertThat(materialRepository.findById(materialId)).isEmpty();
        assertThat(favoriteRepository.existsByUserUsernameIgnoreCaseAndMaterialId(
                viewer.getUsername(), materialId)).isFalse();
        assertThat(shareRepository.findByMaterialIdAndSharedWithId(materialId, viewer.getId())).isEmpty();
    }

    private UserAccount createAccount(String username, String fullName, UserRole role) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setFullName(fullName);
        account.setEmail(username + "@doculib.local");
        account.setPasswordHash(passwordEncoder.encode("KiemThu@123"));
        account.setRole(role);
        account.setEnabled(true);
        return userAccountRepository.save(account);
    }
}
