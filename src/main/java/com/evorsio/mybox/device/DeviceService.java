package com.evorsio.mybox.device;

import com.evorsio.mybox.auth.UserLoggedInEvent;
import com.evorsio.mybox.auth.UserRegisteredEvent;

public interface DeviceService {
    /**
     * 注册阶段：只记录设备信息和指纹，不生成设备令牌
     */
    void registerDevice(UserRegisteredEvent event);

    /**
     * 登录阶段：验证指纹，生成设备令牌
     */
    String loginDeviceAndReturnToken(UserLoggedInEvent event);
}
