package com.openclassrooms.mdd_api.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class SpaCsrfTokenRequestHandlerTest {

    private final SpaCsrfTokenRequestHandler handler = new SpaCsrfTokenRequestHandler();

    @Test
    void handle_delegatesAndSetsRequestAttribute() {
        // Arrange
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        CsrfToken raw = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "RAW");
        Supplier<CsrfToken> supplier = () -> raw;

        // Act
        handler.handle(req, res, supplier);

        // Assert
        Object attr = req.getAttribute(CsrfToken.class.getName());
        assertThat(attr).isInstanceOf(CsrfToken.class);
    }

    @Test
    void resolveCsrfTokenValue_whenHeaderPresent_returnsHeaderValue() {
        // Arrange
        MockHttpServletRequest req = new MockHttpServletRequest();
        CsrfToken raw = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "RAW");

        req.addHeader(raw.getHeaderName(), "RAW");

        // Act
        String resolved = handler.resolveCsrfTokenValue(req, raw);

        // Assert
        assertThat(resolved).isEqualTo("RAW");
    }

    @Test
    void resolveCsrfTokenValue_whenHeaderMissing_usesXorDelegate_path() {
        // Arrange
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        CsrfToken raw = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "RAW");

        // First: let the handler (XOR delegate) generate the masked token in request attributes
        handler.handle(req, res, () -> raw);

        CsrfToken masked = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
        assertThat(masked).isNotNull();

        // Simulate a form POST: parameter contains the (masked) token value
        req.setParameter(raw.getParameterName(), masked.getToken());

        // Act: no header => should go through XOR delegate branch and resolve to the raw token value
        String resolved = handler.resolveCsrfTokenValue(req, raw);

        // Assert
        assertThat(resolved).isEqualTo("RAW");
    }
}
