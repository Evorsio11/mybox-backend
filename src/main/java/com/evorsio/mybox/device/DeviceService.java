package com.evorsio.mybox.device;

import com.evorsio.mybox.auth.UserLoggedInEvent;
import com.evorsio.mybox.auth.UserRegisteredEvent;

import java.util.List;
import java.util.UUID;

public interface DeviceService {
    /**
     * 注册阶段：只记录设备信息和指纹，不生成设备令牌
     */
    void registerDevice(UserRegisteredEvent event);

    /**
     * 登录阶段：验证指纹，生成设备令牌
     */
    String loginDeviceAndReturnToken(UserLoggedInEvent event);

    List<DeviceResponse> listActiveDevices(UUID userId);
    void deleteDevice(UUID userId,UUID deviceId);
    void undoDeleteDevice(UUID userId, UUID deviceId);
    Device heartbeat(UUID userId, UUID deviceId);

    UUID getPrimaryDeviceId(UUID userId);
}
