package com.evorsio.mybox.device.internal.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evorsio.mybox.auth.CurrentUser;
import com.evorsio.mybox.auth.UserPrincipal;
import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.device.Device;
import com.evorsio.mybox.device.DeviceResponse;
import com.evorsio.mybox.device.DeviceService;
import com.evorsio.mybox.device.internal.mapper.DeviceMapper;
import com.evorsio.mybox.device.internal.service.DeviceOnlineStatusService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;
    private final DeviceMapper deviceMapper;
    private final DeviceOnlineStatusService onlineStatusService;

    @GetMapping
    public ApiResponse<List<DeviceResponse>> getDevices(@CurrentUser UserPrincipal user) {
        UUID userId = user.getId();
        List<DeviceResponse> devices = deviceService.listActiveDevices(userId);
        return ApiResponse.success(devices);
    }

    @DeleteMapping("/{deviceId}")
    public ApiResponse<Void> deleteDevice(@CurrentUser UserPrincipal user, @PathVariable UUID deviceId){
        UUID userId = user.getId();
        deviceService.deleteDevice(userId,deviceId);
        return ApiResponse.success();
    }

    @PostMapping("/{deviceId}/heartbeat")
    public ApiResponse<DeviceResponse> heartbeat(@CurrentUser UserPrincipal user,
                                                 @PathVariable UUID deviceId) {
        UUID userId = user.getId();
        Device device = deviceService.heartbeat(userId, deviceId);

        DeviceResponse response = deviceMapper.toResponse(device, onlineStatusService);
        return ApiResponse.success(response);
    }
}
