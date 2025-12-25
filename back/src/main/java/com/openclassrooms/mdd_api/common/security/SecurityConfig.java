package com.openclassrooms.mdd_api.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.mdd_api.common.config.OcAppProperties;
import com.openclassrooms.mdd_api.common.web.ApiErrorCodes;
import com.openclassrooms.mdd_api.common.web.ApiErrorResponse;
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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Security configuration.
 * Règles MVP :
 * - JWT Bearer sur endpoints 🔒
 * - Refresh token via cookie HttpOnly (géré dans AuthController)
 * - CSRF cookie XSRF-TOKEN + header X-XSRF-TOKEN (SPA)
 * */
@Configuration
public class SecurityConfig {

    private static final String API_BASE = "/api";
    private static final String AUTH_BASE = API_BASE + "/auth";

    private static final String CSRF_ENDPOINT = AUTH_BASE + "/csrf";
    private static final String REGISTER_ENDPOINT = AUTH_BASE + "/register";
    private static final String LOGIN_ENDPOINT = AUTH_BASE + "/login";
    private static final String REFRESH_ENDPOINT = AUTH_BASE + "/refresh";
    private static final String LOGOUT_ENDPOINT = AUTH_BASE + "/logout";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            OcAppProperties props
    ) throws Exception {

        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/");
        // Contrat : SameSite=Lax (explicite) ; Secure selon env (HTTP dev vs HTTPS prod)
        csrfRepo.setCookieCustomizer(builder -> builder
                .sameSite("Lax")
                .secure(props.isCookieSecure())
        );

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
                        .requestMatchers(HttpMethod.GET, API_BASE + "/health").permitAll()
                        .requestMatchers(HttpMethod.GET, CSRF_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.POST, REGISTER_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.POST, LOGIN_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.POST, REFRESH_ENDPOINT).permitAll()

                        // 🔒
                        .requestMatchers(HttpMethod.POST, LOGOUT_ENDPOINT).authenticated()

                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(
                                        response, objectMapper, 401,
                                        new ApiErrorResponse(ApiErrorCodes.UNAUTHORIZED, "Unauthorized", List.of())
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(
                                        response, objectMapper, 403,
                                        new ApiErrorResponse(ApiErrorCodes.FORBIDDEN, "Forbidden", List.of())
                                )
                        )
                )

                .build();
    }

    /**
     * Ignore le Bearer sur les endpoints auth "publics" (dont refresh),
     * pour éviter qu'un Authorization expiré casse la requête avant d'arriver au controller.
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        delegate.setAllowUriQueryParameter(false);

        return request -> {
            String path = request.getRequestURI();
            if (path == null) return delegate.resolve(request);

            if (endsWithAny(path, CSRF_ENDPOINT, REGISTER_ENDPOINT, LOGIN_ENDPOINT, REFRESH_ENDPOINT)) {
                return null;
            }
            return delegate.resolve(request);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private static boolean endsWithAny(String path, String... suffixes) {
        for (String s : suffixes) {
            if (path.endsWith(s)) return true;
        }
        return false;
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
