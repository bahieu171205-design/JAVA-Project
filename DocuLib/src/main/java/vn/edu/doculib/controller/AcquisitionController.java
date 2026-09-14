package vn.edu.doculib.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
import vn.edu.doculib.model.AcquisitionRequest;
import vn.edu.doculib.model.AcquisitionStatus;
import vn.edu.doculib.model.Priority;
import vn.edu.doculib.exception.ConcurrentUpdateException;
import vn.edu.doculib.exception.InvalidAcquisitionTransitionException;
import vn.edu.doculib.service.AcquisitionAuditService;
import vn.edu.doculib.service.AcquisitionService;
import vn.edu.doculib.util.InternalReturnUrl;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/acquisitions")
public class AcquisitionController {

    private final AcquisitionService acquisitionService;
    private final AcquisitionAuditService auditService;

    public AcquisitionController(AcquisitionService acquisitionService,
                                 AcquisitionAuditService auditService) {
        this.acquisitionService = acquisitionService;
        this.auditService = auditService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String query,
                       @RequestParam(required = false) AcquisitionStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       HttpServletRequest request,
                       Model model) {
        Page<AcquisitionRequest> requestPage = acquisitionService.search(query, status, page, 10);
        model.addAttribute("requestPage", requestPage);
        model.addAttribute("query", query);
        model.addAttribute("selectedStatus", status);
        Map<Long, java.util.List<AcquisitionStatus>> allowedStatuses = new LinkedHashMap<>();
        requestPage.getContent().forEach(item -> allowedStatuses.put(
                item.getId(), acquisitionService.availableStatuses(item.getStatus())));
        model.addAttribute("allowedStatuses", allowedStatuses);
        model.addAttribute("returnUrl", InternalReturnUrl.currentRequest(request));
        addEnums(model);
        return "acquisitions/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) String returnUrl, Model model) {
        AcquisitionRequest request = new AcquisitionRequest();
        request.setQuantity(1);
        request.setPriority(Priority.MEDIUM);
        request.setStatus(AcquisitionStatus.PROPOSED);
        model.addAttribute("acquisitionRequest", request);
        model.addAttribute("editing", false);
        model.addAttribute("returnUrl", InternalReturnUrl.sanitize(returnUrl, "/acquisitions", "/acquisitions"));
        model.addAttribute("workflowStatuses", acquisitionService.availableStatuses(null));
        addEnums(model);
        return "acquisitions/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam(required = false) String returnUrl,
                           Model model) {
        AcquisitionRequest request = acquisitionService.getById(id);
        model.addAttribute("acquisitionRequest", request);
        model.addAttribute("editing", true);
        model.addAttribute("returnUrl", InternalReturnUrl.sanitize(
                returnUrl, "/acquisitions", "/acquisitions"));
        model.addAttribute("workflowStatuses", acquisitionService.availableStatuses(request.getStatus()));
        model.addAttribute("statusHistory", auditService.findForRequest(id));
        addEnums(model);
        return "acquisitions/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("acquisitionRequest") AcquisitionRequest request,
                       BindingResult bindingResult,
                       @RequestParam(required = false) String returnUrl,
                       Authentication authentication,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", request.getId() != null);
            model.addAttribute("returnUrl", InternalReturnUrl.sanitize(returnUrl, "/acquisitions", "/acquisitions"));
            model.addAttribute("workflowStatuses", request.getId() == null
                    ? acquisitionService.availableStatuses(null)
                    : acquisitionService.availableStatuses(acquisitionService.getById(request.getId()).getStatus()));
            if (request.getId() != null) {
                model.addAttribute("statusHistory", auditService.findForRequest(request.getId()));
            }
            addEnums(model);
            return "acquisitions/form";
        }
        try {
            boolean creating = request.getId() == null;
            acquisitionService.save(request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    creating ? "Đã tạo đề xuất bổ sung" : "Đã cập nhật đề xuất bổ sung");
            return InternalReturnUrl.redirect(returnUrl, "/acquisitions", "/acquisitions");
        } catch (InvalidAcquisitionTransitionException | ConcurrentUpdateException | IllegalStateException exception) {
            bindingResult.reject("workflow", exception.getMessage());
            model.addAttribute("editing", request.getId() != null);
            model.addAttribute("returnUrl", InternalReturnUrl.sanitize(returnUrl, "/acquisitions", "/acquisitions"));
            model.addAttribute("workflowStatuses", request.getId() == null
                    ? acquisitionService.availableStatuses(null)
                    : acquisitionService.availableStatuses(acquisitionService.getById(request.getId()).getStatus()));
            if (request.getId() != null) {
                model.addAttribute("statusHistory", auditService.findForRequest(request.getId()));
            }
            addEnums(model);
            return "acquisitions/form";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam AcquisitionStatus status,
                               @RequestParam(required = false) String note,
                               @RequestParam Long version,
                               @RequestParam(required = false) String returnUrl,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            acquisitionService.updateStatus(id, status, note, version, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đề xuất");
        } catch (InvalidAcquisitionTransitionException | ConcurrentUpdateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return InternalReturnUrl.redirect(returnUrl, "/acquisitions", "/acquisitions");
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         RedirectAttributes redirectAttributes) {
        acquisitionService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đề xuất bổ sung");
        return InternalReturnUrl.redirect(returnUrl, "/acquisitions", "/acquisitions");
    }

    private void addEnums(Model model) {
        model.addAttribute("acquisitionStatuses", AcquisitionStatus.values());
        model.addAttribute("priorities", Priority.values());
    }
}
