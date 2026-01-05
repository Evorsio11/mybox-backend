package com.evorsio.mybox.auth.internal.repository;

import com.evorsio.mybox.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
