package com.openclassrooms.mdd_api.auth.controller;

import com.openclassrooms.mdd_api.auth.dto.IdResponse;
import com.openclassrooms.mdd_api.auth.dto.RegisterRequest;
import com.openclassrooms.mdd_api.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Contrat : 204 + Set-Cookie XSRF-TOKEN
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        // "Récupérer" le token garantit sa matérialisation (cookie via CookieCsrfTokenRepository)
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    // Contrat : POST /register -> 201
    @PostMapping("/register")
    public ResponseEntity<IdResponse> register(@Valid @RequestBody RegisterRequest request) {
        Long id = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
    }
}
