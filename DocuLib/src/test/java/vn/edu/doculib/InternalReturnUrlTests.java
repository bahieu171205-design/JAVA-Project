package vn.edu.doculib;

import org.junit.jupiter.api.Test;
import vn.edu.doculib.util.InternalReturnUrl;

import static org.assertj.core.api.Assertions.assertThat;

class InternalReturnUrlTests {

    @Test
    void acceptsOnlyInternalPathsInsideTheExpectedFeature() {
        assertThat(InternalReturnUrl.sanitize(
                "/materials?page=2&query=java", "/materials", "/materials"))
                .isEqualTo("/materials?page=2&query=java");
        assertThat(InternalReturnUrl.sanitize(
                "https://example.com", "/materials", "/materials"))
                .isEqualTo("/materials");
        assertThat(InternalReturnUrl.sanitize(
                "//example.com/materials", "/materials", "/materials"))
                .isEqualTo("/materials");
        assertThat(InternalReturnUrl.sanitize(
                "/materials/../users", "/materials", "/materials"))
                .isEqualTo("/materials");
        assertThat(InternalReturnUrl.sanitize(
                "/users", "/materials", "/materials"))
                .isEqualTo("/materials");
    }
}
