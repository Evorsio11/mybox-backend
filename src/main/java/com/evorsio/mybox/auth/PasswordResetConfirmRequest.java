package com.evorsio.mybox.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetConfirmRequest {
    @NotBlank(message = "验证码不能为空")
    String token;

    @NotBlank(message = "新密码不能为空")
    String newPassword;
}
