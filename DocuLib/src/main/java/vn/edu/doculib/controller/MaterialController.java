package vn.edu.doculib.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.doculib.dto.MaterialForm;
import vn.edu.doculib.exception.DuplicateCodeException;
import vn.edu.doculib.exception.ConcurrentUpdateException;
import vn.edu.doculib.model.MaterialStatus;
import vn.edu.doculib.model.MaterialType;
import vn.edu.doculib.model.ResourceMaterial;
import vn.edu.doculib.repository.AuthorRepository;
import vn.edu.doculib.repository.CategoryRepository;
import vn.edu.doculib.repository.PublisherRepository;
import vn.edu.doculib.service.MaterialInteractionService;
import vn.edu.doculib.service.MaterialAuditService;
import vn.edu.doculib.service.MaterialService;
import vn.edu.doculib.util.InternalReturnUrl;

import java.time.LocalDate;

@Controller
@RequestMapping("/materials")
public class MaterialController {

    private final MaterialService materialService;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final MaterialInteractionService interactionService;
    private final MaterialAuditService auditService;

    public MaterialController(MaterialService materialService,
                              AuthorRepository authorRepository,
                              CategoryRepository categoryRepository,
                              PublisherRepository publisherRepository,
                              MaterialInteractionService interactionService,
                              MaterialAuditService auditService) {
        this.materialService = materialService;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.interactionService = interactionService;
        this.auditService = auditService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String query,
                       @RequestParam(required = false) MaterialType type,
                       @RequestParam(required = false) MaterialStatus status,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Long authorId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedFrom,
                       @RequestParam(defaultValue = "updated_desc") String sort,
                       @RequestParam(defaultValue = "0") int page,
                       Authentication authentication,
                       HttpServletRequest request,
                       Model model) {
        Page<ResourceMaterial> materialPage = materialService.search(
                query, type, status, categoryId, authorId, updatedFrom, sort, page, 10);
        model.addAttribute("materialPage", materialPage);
        model.addAttribute("query", query);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedCategoryName", categoryId == null ? null
                : categoryRepository.findById(categoryId).map(category -> category.getName()).orElse(null));
        model.addAttribute("selectedAuthorId", authorId);
        model.addAttribute("selectedAuthorName", authorId == null ? null
                : authorRepository.findById(authorId).map(author -> author.getName()).orElse(null));
        model.addAttribute("updatedFrom", updatedFrom);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("favoriteMaterialIds", interactionService.getFavoriteMaterialIds(
                authentication.getName(), materialPage.getContent().stream().map(ResourceMaterial::getId).toList()));
        model.addAttribute("returnUrl", InternalReturnUrl.currentRequest(request));
        addReferenceData(model);
        return "materials/list";
    }

    @GetMapping("/favorites")
    public String favorites(Authentication authentication, Model model) {
        model.addAttribute("collectionType", "favorites");
        model.addAttribute("favorites", interactionService.findFavorites(authentication.getName()));
        return "materials/collection";
    }

    @GetMapping("/shared")
    public String shared(Authentication authentication, Model model) {
        var shares = interactionService.findSharedWith(authentication.getName());
        model.addAttribute("collectionType", "shared");
        model.addAttribute("shares", shares);
        model.addAttribute("favoriteMaterialIds", interactionService.getFavoriteMaterialIds(
                authentication.getName(), shares.stream().map(share -> share.getMaterial().getId()).toList()));
        return "materials/collection";
    }

    @GetMapping("/trash")
    @PreAuthorize("hasRole('ADMIN')")
    public String trash(Model model) {
        model.addAttribute("collectionType", "trash");
        model.addAttribute("trashMaterials", materialService.findTrash());
        return "materials/collection";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) String returnUrl, Model model) {
        MaterialForm form = new MaterialForm();
        form.setMaterialType(MaterialType.BOOK);
        form.setStatus(MaterialStatus.PROCESSING);
        form.setLanguage("Tiếng Việt");
        form.setQuantity(1);
        model.addAttribute("materialForm", form);
        model.addAttribute("editing", false);
        model.addAttribute("returnUrl", InternalReturnUrl.sanitize(returnUrl, "/materials", "/materials"));
        addReferenceData(model);
        return "materials/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         Authentication authentication,
                         Model model) {
        model.addAttribute("material", materialService.getById(id));
        model.addAttribute("favorite", interactionService.isFavorite(id, authentication.getName()));
        model.addAttribute("returnUrl", InternalReturnUrl.sanitize(returnUrl, "/materials", "/materials"));
        if (canEdit(authentication)) {
            model.addAttribute("materialShares", interactionService.findSharesForMaterial(id));
            model.addAttribute("shareRecipients",
                    interactionService.findEligibleRecipients(id, authentication.getName()));
            model.addAttribute("materialAuditLogs", auditService.findForMaterial(id));
        }
        return "materials/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam(required = false) String returnUrl,
                           Model model) {
        model.addAttribute("materialForm", materialService.getForm(id));
        model.addAttribute("editing", true);
        model.addAttribute("returnUrl", InternalReturnUrl.sanitize(
                returnUrl, "/materials/" + id, "/materials"));
        addReferenceData(model);
        return "materials/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("materialForm") MaterialForm form,
                       BindingResult bindingResult,
                       @RequestParam(required = false) String returnUrl,
                       Authentication authentication,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                ResourceMaterial saved = materialService.save(form, authentication.getName());
                redirectAttributes.addFlashAttribute("successMessage",
                        form.getId() == null ? "Đã biên mục tài liệu mới" : "Đã cập nhật biểu ghi tài liệu");
                String fallback = "/materials/" + saved.getId();
                return InternalReturnUrl.redirect(returnUrl, fallback, "/materials");
            } catch (DuplicateCodeException ex) {
                bindingResult.rejectValue("inventoryCode", "duplicate", ex.getMessage());
            } catch (ConcurrentUpdateException ex) {
                bindingResult.reject("concurrent", ex.getMessage());
            }
        }
        model.addAttribute("editing", form.getId() != null);
        model.addAttribute("returnUrl", InternalReturnUrl.sanitize(
                returnUrl, form.getId() == null ? "/materials" : "/materials/" + form.getId(), "/materials"));
        addReferenceData(model);
        return "materials/form";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        materialService.moveToTrash(id, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Đã chuyển tài liệu vào thùng rác");
        return InternalReturnUrl.redirect(returnUrl, "/materials", "/materials");
    }

    @PostMapping("/{id}/favorite")
    public String toggleFavorite(@PathVariable Long id,
                                 @RequestParam(defaultValue = "detail") String returnTo,
                                 @RequestParam(required = false) String returnUrl,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        boolean favorite = interactionService.toggleFavorite(id, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage",
                favorite ? "Đã thêm vào tài liệu yêu thích" : "Đã bỏ khỏi tài liệu yêu thích");
        String legacyFallback = redirectAfterAction(returnTo, id).substring("redirect:".length());
        return InternalReturnUrl.redirect(returnUrl, legacyFallback, "/materials");
    }

    @PostMapping("/{id}/share")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public String share(@PathVariable Long id, @RequestParam Long recipientId,
                        Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            boolean created = interactionService.share(id, recipientId, authentication.getName());
            redirectAttributes.addFlashAttribute(created ? "successMessage" : "errorMessage",
                    created ? "Đã đưa tài liệu vào danh sách Được chia sẻ của người nhận"
                            : "Tài liệu đã có trong danh sách Được chia sẻ của người nhận");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/materials/" + id;
    }

    @PostMapping("/{id}/shares/{recipientId}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public String revokeShare(@PathVariable Long id, @PathVariable Long recipientId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        interactionService.revokeShare(id, recipientId, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Đã gỡ tài liệu khỏi danh sách Được chia sẻ");
        return "redirect:/materials/" + id;
    }

    @PostMapping("/trash/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public String restore(@PathVariable Long id, Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        materialService.restore(id, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục tài liệu vào kho");
        return "redirect:/materials/trash";
    }

    @PostMapping("/trash/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String permanentlyDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        materialService.permanentlyDelete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn tài liệu");
        return "redirect:/materials/trash";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("materialTypes", MaterialType.values());
        model.addAttribute("materialStatuses", MaterialStatus.values());
        model.addAttribute("authors", authorRepository.findAllByOrderByNameAsc());
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("publishers", publisherRepository.findAllByOrderByNameAsc());
    }

    private boolean canEdit(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_LIBRARIAN"));
    }

    private String redirectAfterAction(String returnTo, Long materialId) {
        return switch (returnTo) {
            case "list" -> "redirect:/materials";
            case "favorites" -> "redirect:/materials/favorites";
            case "shared" -> "redirect:/materials/shared";
            default -> "redirect:/materials/" + materialId;
        };
    }
}
