package com.evorsio.mybox.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.springframework.modulith.NamedInterface;

import java.util.UUID;

@Getter
@NamedInterface("common.event")
public class UserRegisteredEvent extends ApplicationEvent {
    private final UUID userId;
    private final UUID deviceId;
    private final String deviceName;
    private final String deviceType;
    private final String osName;
    private final String osVersion;

    public UserRegisteredEvent(Object source, UUID userId, UUID deviceId, String deviceName, String deviceType, String osName, String osVersion) {
        super(source);
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.osName = osName;
        this.osVersion = osVersion;
    }
}
