package com.evorsio.mybox.file.internal.controller;

import com.evorsio.mybox.auth.UserRole;
import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.file.FileConfigResponse;
import com.evorsio.mybox.file.FileConfigUpdateRequest;
import com.evorsio.mybox.file.FileConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<FileConfigResponse> getFileConfig(Authentication authentication) {
        log.info("用户 {} 获取文件配置", authentication.getName());

        FileConfigResponse config = fileConfigService.getFileConfig();
        return ApiResponse.success("获取配置成功", config);
    }

    /**
     * 更新文件配置（仅ADMIN用户可访问）
     * PUT /api/files/config
     */
    @PutMapping
    public ApiResponse<Void> updateFileConfig(
            Authentication authentication,
            @Valid @RequestBody FileConfigUpdateRequest request
    ) {
        // 检查用户权限
        if (!isAdmin(authentication)) {
            log.warn("用户 {} 尝试更新配置但权限不足", authentication.getName());
            return ApiResponse.error("FORBIDDEN", "权限不足，仅管理员可以修改配置");
        }

        log.info("管理员 {} 更新文件配置", authentication.getName());
        fileConfigService.updateFileConfig(request);

        return ApiResponse.success("配置更新成功");
    }

    /**
     * 检查用户是否为管理员
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + UserRole.ADMIN.name()));
    }
}
