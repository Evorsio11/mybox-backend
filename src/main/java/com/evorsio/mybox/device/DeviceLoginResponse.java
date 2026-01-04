package com.evorsio.mybox.device;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class DeviceLoginResponse {
    private UUID deviceId;
    private String deviceToken;
}
