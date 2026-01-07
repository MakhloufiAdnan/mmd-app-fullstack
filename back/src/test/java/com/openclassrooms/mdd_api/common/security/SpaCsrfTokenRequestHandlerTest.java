package com.openclassrooms.mdd_api.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de {@link SpaCsrfTokenRequestHandler}.
 * SUT : SpaCsrfTokenRequestHandler
 * Scope :
 * - handle() délègue et expose un CsrfToken dans la request (usage rendu/form).
 * - resolveCsrfTokenValue():
 *   si header présent -> utilise le token brut (SPA: cookie + header)
 *   si header absent -> n'autorise XOR que si un paramètre CSRF est fourni (form)
 *   si ni header ni param -> retourne null (et Spring Security doit répondre 403)
 */
class SpaCsrfTokenRequestHandlerTest {

    private final SpaCsrfTokenRequestHandler handler = new SpaCsrfTokenRequestHandler();

    @Test
    void handle_delegates_and_sets_request_attribute() {
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
    void resolve_csrf_token_value_when_header_present_returns_header_value() {
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
    void resolve_csrf_token_value_when_header_missing_and_param_missing_returns_null() {
        // Arrange
        MockHttpServletRequest req = new MockHttpServletRequest();
        CsrfToken raw = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "RAW");

        // Act
        String resolved = handler.resolveCsrfTokenValue(req, raw);

        // Assert
        assertThat(resolved).isNull();
    }

    @Test
    void resolve_csrf_token_value_when_header_missing_and_param_present_uses_xor_delegate_path() {
        // Arrange
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        CsrfToken raw = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "RAW");

        // Laisse le handler générer le token masqué (XOR) dans les attributs
        handler.handle(req, res, () -> raw);

        CsrfToken masked = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
        assertThat(masked).isNotNull();

        // Simule un POST de form : paramètre = token masqué
        req.setParameter(raw.getParameterName(), masked.getToken());

        // Act
        String resolved = handler.resolveCsrfTokenValue(req, raw);

        // Assert : doit retrouver le token brut
        assertThat(resolved).isEqualTo("RAW");
    }
}
