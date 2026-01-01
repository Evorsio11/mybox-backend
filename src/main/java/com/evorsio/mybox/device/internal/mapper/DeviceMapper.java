package com.evorsio.mybox.device.internal.mapper;

import com.evorsio.mybox.device.Device;
import com.evorsio.mybox.device.DeviceResponse;
import com.evorsio.mybox.device.OnlineStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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

    @Mapping(target = "onlineStatus", ignore = true)
    DeviceResponse toResponse(Device device);

    List<DeviceResponse> toResponseList(List<Device> devices);

    /**
     * 计算设备在线状态（Domain 层逻辑）
     * 基于最后心跳时间推导状态
     *
     * @param device 设备实体
     * @return 在线状态
     */
    default OnlineStatus calculateOnlineStatus(Device device) {
        if (device.getStatus() != com.evorsio.mybox.device.DeviceStatus.ACTIVE) {
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
