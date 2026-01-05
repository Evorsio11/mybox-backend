package com.evorsio.mybox.auth.internal.controller;

import com.evorsio.mybox.auth.PasswordResetConfirmRequest;
import com.evorsio.mybox.auth.PasswordResetRequest;
import com.evorsio.mybox.auth.PasswordService;
import com.evorsio.mybox.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordController {
    private final PasswordService passwordService;

    @PostMapping("/reset/request")
    private ApiResponse<Void> requestReset(@Valid @RequestBody PasswordResetRequest request){
        passwordService.sendPasswordResetToken(request.getEmail());
        return ApiResponse.success("已发送重置邮件");
    }

    @PostMapping("/reset/confirm")
    private ApiResponse<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request){
        passwordService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.success("密码重置成功");
    }
}
