package com.evorsio.mybox.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class DeviceInfoDto {
    private UUID deviceId;

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    @NotBlank(message = "设备类型不能为空")
    private String deviceType; // DESKTOP / MOBILE / TABLET

    private String osName;
    private String osVersion;
}
