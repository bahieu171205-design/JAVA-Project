package vn.edu.doculib;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.doculib.model.UserAccount;
import vn.edu.doculib.model.UserRole;
import vn.edu.doculib.repository.UserAccountRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAccountSessionSecurityTests {

    private static final String ORIGINAL_PASSWORD = "KiemThu@123";
    private static final List<String> TEST_USERNAMES = List.of(
            "session.role.qa", "session.password.qa", "session.lock.qa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionRegistry sessionRegistry;

    @AfterEach
    void removeTestAccountsAndSessions() {
        for (String username : TEST_USERNAMES) {
            expireAndRemoveSessions(username);
            userAccountRepository.findByUsernameIgnoreCase(username)
                    .ifPresent(userAccountRepository::delete);
        }
    }

    @Test
    void demotingLoggedInLibrarianExpiresOldSessionAndReloadsAuthoritiesOnLogin() throws Exception {
        UserAccount account = createAccount("session.role.qa", UserRole.LIBRARIAN);
        MockHttpSession oldSession = login(account.getUsername(), ORIGINAL_PASSWORD);

        mockMvc.perform(get("/materials/new").session(oldSession))
                .andExpect(status().isOk());

        mockMvc.perform(editAccount(account, UserRole.VIEWER, true)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + account.getId() + "/edit"));

        assertSessionExpired(oldSession);
        mockMvc.perform(get("/materials/new").session(oldSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        MockHttpSession newSession = login(account.getUsername(), ORIGINAL_PASSWORD);
        mockMvc.perform(get("/materials/new").session(newSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void resettingPasswordExpiresOldSessionAndOnlyNewPasswordCanLogin() throws Exception {
        UserAccount account = createAccount("session.password.qa", UserRole.VIEWER);
        MockHttpSession oldSession = login(account.getUsername(), ORIGINAL_PASSWORD);
        String newPassword = "MatKhauMoi@456";

        mockMvc.perform(editAccount(account, UserRole.VIEWER, true)
                        .param("password", newPassword)
                        .param("confirmPassword", newPassword)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertSessionExpired(oldSession);
        mockMvc.perform(get("/").session(oldSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", account.getUsername())
                        .param("password", ORIGINAL_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        login(account.getUsername(), newPassword);
    }

    @Test
    void lockingAccountExpiresOldSessionAndPreventsAnotherLogin() throws Exception {
        UserAccount account = createAccount("session.lock.qa", UserRole.VIEWER);
        MockHttpSession oldSession = login(account.getUsername(), ORIGINAL_PASSWORD);

        mockMvc.perform(post("/users/{id}/toggle", account.getId())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        assertThat(userAccountRepository.findById(account.getId()).orElseThrow().isEnabled()).isFalse();
        assertSessionExpired(oldSession);
        mockMvc.perform(get("/").session(oldSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", account.getUsername())
                        .param("password", ORIGINAL_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    private UserAccount createAccount(String username, UserRole role) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setFullName("Kiểm thử phiên " + username);
        account.setEmail(username + "@doculib.test");
        account.setPasswordHash(passwordEncoder.encode(ORIGINAL_PASSWORD));
        account.setRole(role);
        account.setEnabled(true);
        return userAccountRepository.saveAndFlush(account);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        HttpSession session = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn()
                .getRequest()
                .getSession(false);
        assertThat(session).isInstanceOf(MockHttpSession.class);
        return (MockHttpSession) session;
    }

    private MockHttpServletRequestBuilder editAccount(UserAccount account, UserRole role, boolean enabled) {
        MockHttpServletRequestBuilder request = post("/users/save")
                .param("id", account.getId().toString())
                .param("username", account.getUsername())
                .param("fullName", account.getFullName())
                .param("email", account.getEmail())
                .param("role", role.name());
        if (enabled) {
            request.param("enabled", "true");
        }
        return request;
    }

    private void assertSessionExpired(MockHttpSession session) {
        SessionInformation sessionInformation = sessionRegistry.getSessionInformation(session.getId());
        assertThat(sessionInformation)
                .as("Phiên phải được SessionRegistry theo dõi")
                .isNotNull();
        assertThat(sessionInformation.isExpired()).isTrue();
    }

    private void expireAndRemoveSessions(String username) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof org.springframework.security.core.userdetails.UserDetails)
                .map(principal -> (org.springframework.security.core.userdetails.UserDetails) principal)
                .filter(principal -> principal.getUsername().equalsIgnoreCase(username))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, true).stream())
                .map(SessionInformation::getSessionId)
                .forEach(sessionRegistry::removeSessionInformation);
    }
}
