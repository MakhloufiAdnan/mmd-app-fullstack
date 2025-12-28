package com.openclassrooms.mdd_api.user.service;

import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.user.dto.UpdateMeRequest;
import com.openclassrooms.mdd_api.user.dto.UserMeResponse;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service profil utilisateur.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserMeResponse getMe(Long userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new ApiUnauthorizedException("Unauthorized"));
        // subscriptions : feature topic/subscription à venir -> liste vide pour l’instant
        return new UserMeResponse(u.getId(), u.getEmail(), u.getUsername(), List.of());
    }

    public boolean updateMe(Long userId, UpdateMeRequest req) {
        // To Do : logique update + validations
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
