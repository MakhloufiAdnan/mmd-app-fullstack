package com.openclassrooms.mdd_api.user.repository;

import com.openclassrooms.mdd_api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
}
