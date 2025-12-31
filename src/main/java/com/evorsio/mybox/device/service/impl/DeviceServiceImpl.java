package com.evorsio.mybox.device.service.impl;

import com.evorsio.mybox.device.domain.Device;
import com.evorsio.mybox.device.service.DeviceService;

import java.util.UUID;

public class DeviceServiceImpl implements DeviceService {
    @Override
    public Device findByUserIdAndDeviceId(UUID userId, String deviceId) {
        return null;
    }

    @Override
    public Device registerDevice(UUID userId, Device deviceInfo) {
        return null;
    }

    @Override
    public void updateHeartbeat(UUID deviceId) {

    }

    @Override
    public Device updateDeviceInfo(UUID deviceId, Device deviceInfo) {
        return null;
    }
}
