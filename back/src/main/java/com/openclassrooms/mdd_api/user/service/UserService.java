package com.openclassrooms.mdd_api.user.service;

import com.openclassrooms.mdd_api.auth.validation.PasswordPolicy;
import com.openclassrooms.mdd_api.common.web.exception.ApiBadRequestException;
import com.openclassrooms.mdd_api.common.web.exception.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.common.web.response.FieldErrorItem;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.dto.TopicDto;
import com.openclassrooms.mdd_api.user.dto.UpdateMeRequest;
import com.openclassrooms.mdd_api.user.dto.UserMeResponse;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service profil utilisateur.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserMeResponse getMe(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ApiUnauthorizedException("Unauthorized"));

        List<TopicDto> subscriptions = subscriptionRepository.findSubscribedTopicsByUserId(userId);

        return new UserMeResponse(u.getId(), u.getEmail(), u.getUsername(), subscriptions);
    }

    /**
     * @return true si au moins un champ a changé, false si requête vide / sans changements
     */
    @Transactional
    public boolean updateMe(Long userId, UpdateMeRequest req) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ApiUnauthorizedException("Unauthorized"));

        boolean changed = false;
        changed |= applyEmailUpdate(u, req);
        changed |= applyUsernameUpdate(u, req);
        changed |= applyPasswordUpdate(u, req);

        if (!changed) {
            return false;
        }

        try {
            userRepository.save(u);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Anti-race (double submit / concurrence) : on mappe en 409 sans fuite SQL
            throw new ApiConflictException("Email or username already used");
        }
    }

    private boolean applyEmailUpdate(User u, UpdateMeRequest req) {
        if (req.email() == null) return false;

        String email = normalizeEmailNonNull(req.email());
        if (email.isBlank()) {
            throw validationBlank("email");
        }

        if (email.equals(u.getEmail())) return false;

        if (userRepository.existsByEmailAndIdNot(email, u.getId())) {
            throw new ApiConflictException("Email or username already used");
        }

        u.setEmail(email);
        return true;
    }

    private boolean applyUsernameUpdate(User u, UpdateMeRequest req) {
        if (req.username() == null) return false;

        String username = normalizeUsernameNonNull(req.username());
        if (username.isBlank()) {
            throw validationBlank("username");
        }

        if (username.equals(u.getUsername())) return false;

        if (userRepository.existsByUsernameAndIdNot(username, u.getId())) {
            throw new ApiConflictException("Email or username already used");
        }

        u.setUsername(username);
        return true;
    }

    private boolean applyPasswordUpdate(User u, UpdateMeRequest req) {
        if (req.password() == null) return false;

        if (!PasswordPolicy.isValid(req.password())) {
            throw new ApiBadRequestException(
                    "Password policy not respected",
                    List.of(new FieldErrorItem(
                            "password",
                            "Must be >=8 and contain lower, upper, digit and special character"
                    ))
            );
        }

        u.setPasswordHash(passwordEncoder.encode(req.password()));
        return true;
    }

    private static ApiBadRequestException validationBlank(String field) {
        return new ApiBadRequestException("Validation error",
                List.of(new FieldErrorItem(field, "Must not be blank")));
    }

    private static String normalizeEmailNonNull(String email) {
        return email.trim().toLowerCase();
    }

    private static String normalizeUsernameNonNull(String username) {
        return username.trim();
    }
}
