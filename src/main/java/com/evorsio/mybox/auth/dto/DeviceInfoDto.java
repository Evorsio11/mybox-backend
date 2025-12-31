package com.evorsio.mybox.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class DeviceInfoDto {
    @NotBlank(message = "设备 UUID 不能为空")
    private UUID deviceId;

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    @NotBlank(message = "设备类型不能为空")
    private String deviceType; // DESKTOP / MOBILE / TABLET

    private String osName;
    private String osVersion;
}
