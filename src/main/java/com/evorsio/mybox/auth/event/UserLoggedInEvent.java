package com.evorsio.mybox.auth.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserLoggedInEvent{
    private final UUID userId;
    private final String deviceId;
    private final String deviceName;
    private final String deviceType;
    private final String osName;
    private final String osVersion;
}
