package com.evorsio.mybox.user.controller;

import com.evorsio.mybox.api.response.ApiResponse;
import com.evorsio.mybox.user.dto.UserInfoResponse;
import com.evorsio.mybox.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser(Authentication authentication) {
        // 从 JWT 获取用户 ID
        Object details = authentication.getDetails();
        UUID userId = UUID.fromString(details.toString());

        UserInfoResponse userInfo = userService.getUserInfo(userId);
        return ApiResponse.success("获取用户信息成功", userInfo);
    }
}
