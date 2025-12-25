package com.evorsio.mybox.auth.service.impl;

import com.evorsio.mybox.auth.domain.User;
import com.evorsio.mybox.auth.domain.UserRole;
import com.evorsio.mybox.auth.exception.EmailAlreadyExistsException;
import com.evorsio.mybox.auth.exception.InvalidCredentialsException;
import com.evorsio.mybox.auth.exception.UserNotFoundException;
import com.evorsio.mybox.auth.exception.UsernameAlreadyExistsException;
import com.evorsio.mybox.auth.repository.UserRepository;
import com.evorsio.mybox.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);

        UserRole role;
        if (userRepository.count() == 0) {
            role = UserRole.ADMIN;
        } else {
            role = UserRole.USER;
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(encodedPassword)
                .role(role)
                .build();

        return userRepository.save(user);
    }

    @Override
    public User login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Override
    public boolean validateUser(String username, String password) {
        return userRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }
}
