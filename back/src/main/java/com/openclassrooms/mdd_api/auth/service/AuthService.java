package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.dto.RegisterRequest;
import com.openclassrooms.mdd_api.auth.validation.PasswordPolicy;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(RegisterRequest req) {
        if (!PasswordPolicy.isValid(req.password())) {
            throw new IllegalArgumentException("Password policy not respected");
        }

        if (userRepository.existsByEmail(req.email()) || userRepository.existsByUsername(req.username())) {
            throw new IllegalStateException("Email or username already used");
        }

        User u = new User(req.email(), req.username(), passwordEncoder.encode(req.password()));
        return userRepository.save(u).getId();
    }
}
