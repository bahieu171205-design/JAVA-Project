package vn.edu.doculib.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.doculib.dto.MaterialForm;
import vn.edu.doculib.exception.DuplicateCodeException;
import vn.edu.doculib.exception.ConcurrentUpdateException;
import vn.edu.doculib.exception.ResourceNotFoundException;
import vn.edu.doculib.model.Author;
import vn.edu.doculib.model.MaterialStatus;
import vn.edu.doculib.model.MaterialAuditAction;
import vn.edu.doculib.model.MaterialType;
import vn.edu.doculib.model.ResourceMaterial;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.repository.AuthorRepository;
import vn.edu.doculib.repository.CategoryRepository;
import vn.edu.doculib.repository.MaterialFavoriteRepository;
import vn.edu.doculib.repository.MaterialShareRepository;
import vn.edu.doculib.repository.PublisherRepository;
import vn.edu.doculib.repository.ResourceMaterialRepository;
import vn.edu.doculib.repository.UserAccountRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class MaterialService {

    private final ResourceMaterialRepository materialRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final UserAccountRepository userAccountRepository;
    private final MaterialFavoriteRepository favoriteRepository;
    private final MaterialShareRepository shareRepository;
    private final BusinessCodeService businessCodeService;
    private final MaterialAuditService auditService;

    public MaterialService(ResourceMaterialRepository materialRepository,
                           AuthorRepository authorRepository,
                           CategoryRepository categoryRepository,
                           PublisherRepository publisherRepository,
                           UserAccountRepository userAccountRepository,
                           MaterialFavoriteRepository favoriteRepository,
                           MaterialShareRepository shareRepository,
                           BusinessCodeService businessCodeService,
                           MaterialAuditService auditService) {
        this.materialRepository = materialRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.userAccountRepository = userAccountRepository;
        this.favoriteRepository = favoriteRepository;
        this.shareRepository = shareRepository;
        this.businessCodeService = businessCodeService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ResourceMaterial> search(String query, MaterialType type, MaterialStatus status,
                                         Long categoryId, Long authorId, LocalDate updatedFrom,
                                         String sort, int page, int size) {
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        LocalDateTime updatedFromDateTime = updatedFrom == null ? null : updatedFrom.atStartOfDay();
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), size, resolveSort(sort));
        return materialRepository.search(
                normalizedQuery, type, status, categoryId, authorId, updatedFromDateTime, pageRequest);
    }

    @Transactional(readOnly = true)
    public ResourceMaterial getById(Long id) {
        return materialRepository.findByIdAndTrashedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu #" + id));
    }

    @Transactional(readOnly = true)
    public MaterialForm getForm(Long id) {
        return MaterialForm.fromEntity(getById(id));
    }

    @Transactional
    public ResourceMaterial save(MaterialForm form, String actorUsername) {
        boolean creating = form.getId() == null;
        ResourceMaterial material = creating
                ? new ResourceMaterial()
                : getById(form.getId());
        if (!creating && !Objects.equals(form.getVersion(), material.getVersion())) {
            throw new ConcurrentUpdateException(
                    "Tài liệu đã được người khác cập nhật. Hãy tải lại trang và kiểm tra thay đổi mới nhất.");
        }

        String code = normalizeCode(form.getInventoryCode());
        if (!StringUtils.hasText(code)) {
            code = businessCodeService.nextMaterialCode();
        }
        boolean duplicated = material.getId() == null
                ? materialRepository.existsByInventoryCodeIgnoreCase(code)
                : materialRepository.existsByInventoryCodeIgnoreCaseAndIdNot(code, material.getId());
        if (duplicated) {
            throw new DuplicateCodeException("Mã tài liệu đã tồn tại trong hệ thống");
        }

        material.setInventoryCode(code);
        material.setTitle(form.getTitle().trim());
        material.setIsbnIssn(trimToNull(form.getIsbnIssn()));
        material.setMaterialType(form.getMaterialType());
        material.setStatus(form.getStatus());
        material.setLanguage(trimToNull(form.getLanguage()));
        material.setPublishYear(form.getPublishYear());
        material.setEdition(trimToNull(form.getEdition()));
        material.setCallNumber(trimToNull(form.getCallNumber()));
        material.setPages(form.getPages());
        material.setQuantity(form.getQuantity());
        material.setDescription(trimToNull(form.getDescription()));
        material.setCategory(form.getCategoryId() == null ? null : categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Chủ đề không tồn tại")));
        material.setPublisher(form.getPublisherId() == null ? null : publisherRepository.findById(form.getPublisherId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhà xuất bản không tồn tại")));

        Set<Long> authorIds = form.getAuthorIds() == null ? Set.of() : form.getAuthorIds();
        List<Author> authors = authorRepository.findAllById(authorIds);
        if (authors.size() != authorIds.size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều tác giả không tồn tại");
        }
        material.setAuthors(new LinkedHashSet<>(authors));
        try {
            ResourceMaterial saved = materialRepository.saveAndFlush(material);
            auditService.record(saved,
                    creating ? MaterialAuditAction.CREATED : MaterialAuditAction.UPDATED,
                    actorUsername,
                    creating ? "Tạo biểu ghi tài liệu" : "Cập nhật thông tin biên mục");
            return saved;
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ConcurrentUpdateException(
                    "Tài liệu đã được người khác cập nhật. Hãy tải lại trang và thử lại.", exception);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCodeException("Mã tài liệu vừa được tài khoản khác sử dụng; hãy thử lưu lại");
        }
    }

    @Transactional
    public void moveToTrash(Long id, String username) {
        ResourceMaterial material = getById(id);
        UserAccount account = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập"));
        material.setTrashed(true);
        material.setTrashedAt(LocalDateTime.now());
        material.setTrashedBy(account);
        materialRepository.saveAndFlush(material);
        auditService.record(material, MaterialAuditAction.MOVED_TO_TRASH, username,
                "Chuyển tài liệu vào thùng rác");
    }

    @Transactional(readOnly = true)
    public List<ResourceMaterial> findTrash() {
        return materialRepository.findAllByTrashedTrueOrderByTrashedAtDesc();
    }

    @Transactional
    public void restore(Long id, String actorUsername) {
        ResourceMaterial material = getTrashedById(id);
        material.setTrashed(false);
        material.setTrashedAt(null);
        material.setTrashedBy(null);
        materialRepository.saveAndFlush(material);
        auditService.record(material, MaterialAuditAction.RESTORED, actorUsername,
                "Khôi phục tài liệu vào kho dùng chung");
    }

    @Transactional
    public void permanentlyDelete(Long id) {
        ResourceMaterial material = getTrashedById(id);
        favoriteRepository.deleteAllByMaterialId(id);
        shareRepository.deleteAllByMaterialId(id);
        materialRepository.delete(material);
    }

    private ResourceMaterial getTrashedById(Long id) {
        return materialRepository.findByIdAndTrashedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu trong thùng rác #" + id));
    }

    private String normalizeCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Sort resolveSort(String sort) {
        return switch (sort == null ? "updated_desc" : sort) {
            case "updated_asc" -> Sort.by(Sort.Direction.ASC, "updatedAt");
            case "title_asc" -> Sort.by(Sort.Direction.ASC, "title");
            case "title_desc" -> Sort.by(Sort.Direction.DESC, "title");
            default -> Sort.by(Sort.Direction.DESC, "updatedAt");
        };
    }
}
