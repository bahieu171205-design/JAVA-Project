package vn.edu.doculib.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class OnlineUserService {

    private final SessionRegistry sessionRegistry;

    public OnlineUserService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public Set<String> getOnlineUsernames(String currentUsername) {
        Set<String> usernames = new LinkedHashSet<>();
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (sessionRegistry.getAllSessions(principal, false).isEmpty()) {
                continue;
            }
            String username = extractUsername(principal);
            if (username != null) {
                usernames.add(normalize(username));
            }
        }
        if (currentUsername != null && !currentUsername.isBlank()) {
            usernames.add(normalize(currentUsername));
        }
        return Set.copyOf(usernames);
    }

    public void expireSessions(String username) {
        String normalizedUsername = normalize(username);
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String principalUsername = extractUsername(principal);
            if (principalUsername == null || !normalize(principalUsername).equals(normalizedUsername)) {
                continue;
            }
            sessionRegistry.getAllSessions(principal, false).forEach(session -> session.expireNow());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void expireSessionsAfterAccountSecurityChange(UserAccountSecurityChangedEvent event) {
        expireSessions(event.username());
    }

    private String extractUsername(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof Authentication authentication) {
            return authentication.getName();
        }
        return null;
    }

    private String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
