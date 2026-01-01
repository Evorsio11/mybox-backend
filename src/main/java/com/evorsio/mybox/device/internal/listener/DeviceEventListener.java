package com.evorsio.mybox.device.internal.listener;

import com.evorsio.mybox.auth.UserLoggedInEvent;
import com.evorsio.mybox.auth.UserRegisteredEvent;
import com.evorsio.mybox.device.DeviceService;
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
