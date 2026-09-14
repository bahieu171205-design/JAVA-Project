package vn.edu.doculib.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.doculib.dto.UserAccountForm;
import vn.edu.doculib.exception.UserAccountValidationException;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.model.UserRole;
import vn.edu.doculib.service.OnlineUserService;
import vn.edu.doculib.service.UserAccountService;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final OnlineUserService onlineUserService;

    public UserAccountController(UserAccountService userAccountService, OnlineUserService onlineUserService) {
        this.userAccountService = userAccountService;
        this.onlineUserService = onlineUserService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("accounts", userAccountService.findAll());
        model.addAttribute("onlineUsernames", onlineUserService.getOnlineUsernames(authentication.getName()));
        return "users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        UserAccountForm form = new UserAccountForm();
        form.setRole(UserRole.VIEWER);
        form.setEnabled(true);
        model.addAttribute("userAccountForm", form);
        model.addAttribute("editing", false);
        addRoles(model);
        return "users/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("userAccountForm", userAccountService.getForm(id));
        model.addAttribute("editing", true);
        addRoles(model);
        return "users/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("userAccountForm") UserAccountForm form,
                       BindingResult bindingResult,
                       Authentication authentication,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                UserAccount saved = userAccountService.save(form, authentication.getName());
                redirectAttributes.addFlashAttribute("successMessage",
                        form.getId() == null ? "Đã tạo tài khoản mới" : "Đã cập nhật tài khoản");
                return "redirect:/users/" + saved.getId() + "/edit";
            } catch (UserAccountValidationException exception) {
                bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
            }
        }
        model.addAttribute("editing", form.getId() != null);
        addRoles(model);
        return "users/form";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            boolean enabled = userAccountService.toggleEnabled(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    enabled ? "Đã kích hoạt tài khoản" : "Đã vô hiệu hóa tài khoản");
        } catch (UserAccountValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/users";
    }

    private void addRoles(Model model) {
        model.addAttribute("userRoles", UserRole.values());
    }
}
