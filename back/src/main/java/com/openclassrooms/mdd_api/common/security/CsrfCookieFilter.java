package com.openclassrooms.mdd_api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Force l'émission du cookie CSRF (XSRF-TOKEN) dans les réponses.
 * Spring Security peut générer le token CSRF en "deferred".
 * Lire le token déclenche sa matérialisation -> Set-Cookie XSRF-TOKEN.
 */
public final class CsrfCookieFilter extends OncePerRequestFilter {

    private static final String LEGACY_ATTR_NAME = "_csrf";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) request.getAttribute(LEGACY_ATTR_NAME);
        }

        if (csrfToken != null) {
            csrfToken.getToken();
        }

        filterChain.doFilter(request, response);
    }
}
