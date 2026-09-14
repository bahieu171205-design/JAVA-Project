package vn.edu.doculib.service;

/**
 * Signals that previously authenticated sessions must no longer use the
 * security state cached for an account.
 */
public record UserAccountSecurityChangedEvent(String username) {
}
