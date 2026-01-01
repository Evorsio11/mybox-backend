package com.evorsio.mybox.user.internal.service.impl;

import com.evorsio.mybox.auth.User;
import com.evorsio.mybox.user.UserInfoResponse;
import com.evorsio.mybox.user.internal.repository.UserRepository;
import com.evorsio.mybox.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserInfoResponse getUserInfo(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户未找到:" + id));

        return new UserInfoResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail()
        );
    }
}
