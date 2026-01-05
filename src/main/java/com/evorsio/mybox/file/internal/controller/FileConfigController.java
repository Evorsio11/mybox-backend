package com.evorsio.mybox.file.internal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evorsio.mybox.auth.CurrentUser;
import com.evorsio.mybox.auth.UserPrincipal;
import com.evorsio.mybox.auth.UserRole;
import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.file.FileConfigResponse;
import com.evorsio.mybox.file.FileConfigService;
import com.evorsio.mybox.file.FileConfigUpdateRequest;
import com.evorsio.mybox.file.internal.exception.FileException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件配置控制器
 * 提供文件上传和下载配置的查询和更新接口
 */
@Slf4j
@RestController
@RequestMapping("/api/files/config")
@RequiredArgsConstructor
public class FileConfigController {
    private final FileConfigService fileConfigService;

    /**
     * 获取文件配置（所有认证用户可访问）
     * GET /api/files/config
     */
    @GetMapping
    public ApiResponse<FileConfigResponse> getFileConfig(@CurrentUser UserPrincipal user) {
        log.info("用户 {} 获取文件配置", user.getUsername());

        FileConfigResponse config = fileConfigService.getFileConfig();
        return ApiResponse.success("获取配置成功", config);
    }

    /**
     * 更新文件配置（仅ADMIN用户可访问）
     * PUT /api/files/config
     */
    @PutMapping
    public ApiResponse<Void> updateFileConfig(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody FileConfigUpdateRequest request
    ) {
        // 检查用户权限
        if (!isAdmin(user)) {
            log.warn("用户 {} 尝试更新配置但权限不足", user.getUsername());
            throw new FileException(ErrorCode.ACCESS_DENIED);
        }

        log.info("管理员 {} 更新文件配置", user.getUsername());
        fileConfigService.updateFileConfig(request);

        return ApiResponse.success();
    }

    /**
     * 检查用户是否为管理员
     */
    private boolean isAdmin(UserPrincipal user) {
        return user.getRole() == UserRole.ADMIN;
    }
}
