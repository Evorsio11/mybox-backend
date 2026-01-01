package com.evorsio.mybox.device.internal.mapper;

import com.evorsio.mybox.device.Device;
import com.evorsio.mybox.device.DeviceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DeviceMapper {

    @Mapping(target = "onlineStatus", expression = "java(device.getOnlineStatus())")
    DeviceResponse toResponse(Device device);

    List<DeviceResponse> toResponseList(List<Device> devices);
}
