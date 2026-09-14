package vn.edu.doculib.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;
import vn.edu.doculib.exception.ResourceNotFoundException;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.repository.UserAccountRepository;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserAccountRepository userAccountRepository;

    public GlobalControllerAdvice(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @ModelAttribute
    public void commonAttributes(Model model, HttpServletRequest request, Authentication authentication) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("currentView", request.getParameter("view"));
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        boolean isAdmin = authenticated && hasRole(authentication, "ROLE_ADMIN");
        boolean canEdit = isAdmin || (authenticated && hasRole(authentication, "ROLE_LIBRARIAN"));
        UserAccount currentUser = authenticated
                ? userAccountRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null)
                : null;
        model.addAttribute("authenticated", authenticated);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("canEdit", canEdit);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException exception) {
        ModelAndView modelAndView = new ModelAndView("error/404");
        modelAndView.setStatus(org.springframework.http.HttpStatus.NOT_FOUND);
        modelAndView.addObject("errorMessage", exception.getMessage());
        return modelAndView;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
