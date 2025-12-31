package com.evorsio.mybox.device.listener;

import com.evorsio.mybox.common.event.UserLoggedInEvent;
import com.evorsio.mybox.common.event.UserRegisteredEvent;
import com.evorsio.mybox.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceEventListener {
    private final DeviceService deviceService;

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        deviceService.registerDevice(event);
    }

    @EventListener
    public void handleUserLoggedIn(UserLoggedInEvent event) {
        String deviceToken = deviceService.loginDeviceAndReturnToken(event);
        event.getDeviceTokenFuture().complete(deviceToken);
    }
}
