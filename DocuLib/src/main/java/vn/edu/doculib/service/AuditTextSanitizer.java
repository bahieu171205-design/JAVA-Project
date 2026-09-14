package vn.edu.doculib.service;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

final class AuditTextSanitizer {

    private static final int MAX_LENGTH = 1000;
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?iu)(password|passwd|pwd|token|session(?:id)?|secret|mật\\s*khẩu|mã\\s*phiên)\\s*[:=]\\s*([^,;\\s]+)");

    private AuditTextSanitizer() {
    }

    static String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", "");
        String redacted = SECRET_ASSIGNMENT.matcher(normalized).replaceAll("$1=[đã ẩn]");
        return redacted.length() <= MAX_LENGTH ? redacted : redacted.substring(0, MAX_LENGTH);
    }

    static String actor(String username) {
        if (!StringUtils.hasText(username)) {
            return "system";
        }
        String normalized = username.trim();
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100);
    }
}
