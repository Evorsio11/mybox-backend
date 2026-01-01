package com.evorsio.mybox.user.internal.repository;

import com.evorsio.mybox.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findById(UUID id);
}
