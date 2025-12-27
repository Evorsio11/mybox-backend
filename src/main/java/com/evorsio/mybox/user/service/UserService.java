package com.evorsio.mybox.user.service;

import com.evorsio.mybox.user.dto.UserInfoResponse;

import java.util.UUID;

public interface UserService {
    UserInfoResponse getUserInfo(UUID id);
}
