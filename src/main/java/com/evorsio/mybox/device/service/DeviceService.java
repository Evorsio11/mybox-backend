package com.evorsio.mybox.device.service;

import com.evorsio.mybox.device.domain.Device;

import java.util.UUID;

public interface DeviceService {
    Device findByUserIdAndDeviceId(UUID userId,String deviceId);
    Device registerDevice(UUID userId,Device deviceInfo);
    void updateHeartbeat(UUID deviceId);
    Device updateDeviceInfo(UUID deviceId,Device deviceInfo);
}
