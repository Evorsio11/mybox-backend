package com.evorsio.mybox.device.internal.controller;

import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.device.Device;
import com.evorsio.mybox.device.DeviceResponse;
import com.evorsio.mybox.device.DeviceService;
import com.evorsio.mybox.device.internal.mapper.DeviceMapper;
import com.evorsio.mybox.device.internal.service.DeviceOnlineStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;
    private final DeviceMapper deviceMapper;
    private final DeviceOnlineStatusService onlineStatusService;

    @GetMapping
    public ApiResponse<List<DeviceResponse>> getDevices(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<DeviceResponse> devices = deviceService.listActiveDevices(userId);
        return ApiResponse.success(devices);
    }

    @DeleteMapping("/{deviceId}")
    public ApiResponse<Void> deleteDevice(Authentication authentication, @PathVariable UUID deviceId){
        UUID userId = (UUID) authentication.getPrincipal();
        deviceService.deleteDevice(userId,deviceId);
        return ApiResponse.success();
    }

    @PostMapping("/{deviceId}/heartbeat")
    public ApiResponse<DeviceResponse> heartbeat(Authentication authentication,
                                                 @PathVariable UUID deviceId) {
        UUID userId = (UUID) authentication.getPrincipal();
        Device device = deviceService.heartbeat(userId, deviceId);

        DeviceResponse response = deviceMapper.toResponse(device, onlineStatusService);
        return ApiResponse.success(response);
    }
}
