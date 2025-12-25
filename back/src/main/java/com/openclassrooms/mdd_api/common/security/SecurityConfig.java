package com.openclassrooms.mdd_api.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.mdd_api.common.web.ApiErrorCodes;
import com.openclassrooms.mdd_api.common.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

import java.io.IOException;
import java.util.List;

/**
 * Security configuration (Spring Security 6.x).
 * Contrat MVP :
 * - JWT Bearer pour endpoints 🔒
 * - refresh token en cookie HttpOnly
 * - CSRF cookie XSRF-TOKEN + header X-XSRF-TOKEN
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        // Cookie CSRF lisible JS (Angular/Postman) => cookie "XSRF-TOKEN"
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/"); // Path=/ comme dans l'exemple du contrat

        return http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // CSRF SPA (actif pour tous POST/PUT/DELETE)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                // Force l’émission du cookie XSRF-TOKEN (tokens deferred)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)

                // API stateless : pas de session côté serveur
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Validation JWT Bearer (Resource Server)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(Customizer.withDefaults())
                )

                .authorizeHttpRequests(auth -> auth
                        // Préflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()

                        // Logout : endpoint protégé (🔒)
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()

                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                /**
                 * Erreurs Security au format contrat :
                 * - 401 si non authentifié
                 * - 403 si authentifié mais interdit (ou CSRF manquant/invalide)
                 */
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(
                                        response,
                                        objectMapper,
                                        401,
                                        new ApiErrorResponse(ApiErrorCodes.UNAUTHORIZED, "Unauthorized", List.of())
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(
                                        response,
                                        objectMapper,
                                        403,
                                        new ApiErrorResponse(ApiErrorCodes.FORBIDDEN, "Forbidden", List.of())
                                )
                        )
                )

                .build();
    }

    /**
     * Résout le Bearer token (Authorization header).
     *
     * Cas réel en SPA/Postman :
     * - /api/auth/refresh est "public"
     * - mais si un Authorization expiré est envoyé, Spring peut échouer AVANT le controller
     *
     * Solution :
     * - ignorer le Bearer sur /api/auth/csrf|register|login|refresh
     * - MAIS conserver le Bearer sur /api/auth/logout (🔒)
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return request -> {
            String path = request.getRequestURI();
            if (path == null) return delegate.resolve(request);

            if (path.equals("/api/auth/csrf")
                    || path.equals("/api/auth/register")
                    || path.equals("/api/auth/login")
                    || path.equals("/api/auth/refresh")) {
                return null;
            }
            return delegate.resolve(request);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Delegating encoder -> {bcrypt}... (évolutif et recommandé)
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private static void writeJsonError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            ApiErrorResponse body
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
