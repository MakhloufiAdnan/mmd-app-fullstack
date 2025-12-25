package com.openclassrooms.mdd_api.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * CSRF handler adapté SPA.
 * - SPA (Angular) : lit cookie XSRF-TOKEN et renvoie header X-XSRF-TOKEN (token "brut").
 * - Pour conserver la protection BREACH côté rendu/form, on garde le XOR handler.
 * Comportement :
 * - header présent => compare token brut (header)
 * - sinon => logique XOR (form param / autres cas)
 */
public final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        // Toujours utiliser XOR handler pour conserver la protection BREACH sur le rendu
        this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // Si header présent (SPA), on compare avec le token brut (cookie XSRF-TOKEN)
        if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
            return super.resolveCsrfTokenValue(request, csrfToken);
        }
        // Sinon (form param), garder la logique XOR
        return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
