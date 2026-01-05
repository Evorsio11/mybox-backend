package com.evorsio.mybox.user.internal.controller;

import com.evorsio.mybox.auth.CurrentUser;
import com.evorsio.mybox.auth.UserPrincipal;
import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.user.UserInfoResponse;
import com.evorsio.mybox.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser(@CurrentUser UserPrincipal user) {
        // ✅ 直接通过 @CurrentUser 注解获取用户信息，包含 ID、用户名、角色等
        UserInfoResponse userInfo = userService.getUserInfo(user.getId());
        return ApiResponse.success("获取用户信息成功", userInfo);
    }
}
