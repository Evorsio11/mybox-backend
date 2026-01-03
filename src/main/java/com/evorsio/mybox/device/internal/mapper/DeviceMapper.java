package com.evorsio.mybox.device.internal.mapper;

import com.evorsio.mybox.device.Device;
import com.evorsio.mybox.device.DeviceResponse;
import com.evorsio.mybox.device.DeviceStatus;
import com.evorsio.mybox.device.OnlineStatus;
import com.evorsio.mybox.device.internal.service.DeviceOnlineStatusService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DeviceMapper {

    DeviceResponse toResponse(Device device, @Context DeviceOnlineStatusService onlineStatusService);

    List<DeviceResponse> toResponseList(List<Device> devices, @Context DeviceOnlineStatusService onlineStatusService);

    /**
     * 映射后设置在线状态
     * 优先使用 Redis 中的心跳时间，如果没有则使用数据库中的心跳时间
     */
    @AfterMapping
    default void setOnlineStatus(@MappingTarget DeviceResponse response,
                                Device device,
                                @Context DeviceOnlineStatusService onlineStatusService) {
        LocalDateTime redisHeartbeat = onlineStatusService.getLastHeartbeat(device.getDeviceId());

        if (redisHeartbeat != null) {
            LocalDateTime originalHeartbeat = device.getLastHeartbeat();
            device.setLastHeartbeat(redisHeartbeat);
            response.setOnlineStatus(calculateOnlineStatus(device));
            device.setLastHeartbeat(originalHeartbeat); // 恢复原值
        } else {
            // 使用数据库中的心跳时间计算状态
            response.setOnlineStatus(calculateOnlineStatus(device));
        }
    }

    /**
     * 计算设备在线状态（Domain 层逻辑）
     * 基于最后心跳时间推导状态
     */
    default OnlineStatus calculateOnlineStatus(Device device) {
        if (device.getStatus() != DeviceStatus.ACTIVE) {
            return OnlineStatus.OFFLINE;
        }

        LocalDateTime lastHeartbeat = device.getLastHeartbeat();
        if (lastHeartbeat == null) {
            return OnlineStatus.OFFLINE;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1分钟内有心跳：在线
        if (lastHeartbeat.isAfter(now.minusMinutes(1))) {
            return OnlineStatus.ONLINE;
        }
        // 5分钟内有心跳：休眠
        if (lastHeartbeat.isAfter(now.minusMinutes(5))) {
            return OnlineStatus.SLEEPING;
        }
        // 超过5分钟：离线
        return OnlineStatus.OFFLINE;
    }
}
