package com.evorsio.mybox.device;

import com.evorsio.mybox.auth.DeviceInfoDto;

import java.util.List;
import java.util.UUID;

public interface DeviceService {
    /**
     * 注册阶段：只记录设备信息和指纹，不生成设备令牌
     * 后端生成deviceId
     */
    void registerDevice(UUID userId, DeviceInfoDto deviceInfo);

    /**
     * 登录阶段：验证指纹，生成设备令牌
     * 后端生成deviceId
     */
    DeviceLoginResponse loginDeviceAndReturnToken(UUID userId, DeviceInfoDto deviceInfo);

    List<DeviceResponse> listActiveDevices(UUID userId);
    void deleteDevice(UUID userId,UUID deviceId);
    void undoDeleteDevice(UUID userId, UUID deviceId);
    Device heartbeat(UUID userId, UUID deviceId);

    UUID getPrimaryDeviceId(UUID userId);
}
