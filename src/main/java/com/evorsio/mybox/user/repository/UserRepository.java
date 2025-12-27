package com.evorsio.mybox.user.repository;

import com.evorsio.mybox.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findById(UUID id);
}
