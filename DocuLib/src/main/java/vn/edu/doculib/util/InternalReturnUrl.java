package vn.edu.doculib.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URISyntaxException;

public final class InternalReturnUrl {

    private InternalReturnUrl() {
    }

    public static String currentRequest(HttpServletRequest request) {
        String query = request.getQueryString();
        return request.getRequestURI() + (query == null || query.isBlank() ? "" : "?" + query);
    }

    public static String sanitize(String candidate, String fallback, String allowedRoot) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        String value = candidate.trim();
        if (!value.startsWith("/") || value.startsWith("//") || value.contains("\\")
                || value.contains("\r") || value.contains("\n") || value.contains("..")) {
            return fallback;
        }
        try {
            URI uri = new URI(value);
            String path = uri.getPath();
            boolean internal = !uri.isAbsolute() && uri.getRawAuthority() == null
                    && uri.getFragment() == null && path != null
                    && (path.equals(allowedRoot) || path.startsWith(allowedRoot + "/"));
            return internal ? value : fallback;
        } catch (URISyntaxException exception) {
            return fallback;
        }
    }

    public static String redirect(String candidate, String fallback, String allowedRoot) {
        return "redirect:" + sanitize(candidate, fallback, allowedRoot);
    }
}
