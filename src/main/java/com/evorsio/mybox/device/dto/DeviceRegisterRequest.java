package com.evorsio.mybox.device.dto;

import com.evorsio.mybox.device.domain.DeviceType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 设备注册请求 DTO
 * 客户端在登录时携带此信息
 */
@Data
public class DeviceRegisterRequest {

    /**
     * 设备唯一标识（客户端生成并持久化）
     */
    @NotBlank(message = "设备UUID不能为空")
    private String deviceUuid;

    /**
     * 设备名称（用户可自定义，如"我的iPhone"、"办公电脑"）
     */
    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    /**
     * 设备类型
     */
    private DeviceType deviceType;

    /**
     * 操作系统名称（如 Windows、macOS、iOS、Android）
     */
    private String osName;

    /**
     * 操作系统版本（如 Windows 11、iOS 17.5）
     */
    private String osVersion;

    /**
     * 设备指纹（可选，基于硬件信息生成，用于安全验证）
     */
    private String fingerprint;
}
