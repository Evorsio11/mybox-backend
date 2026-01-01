package com.evorsio.mybox.device.internal.repository;

import com.evorsio.mybox.device.Device;
import com.evorsio.mybox.device.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByUserIdAndDeviceId(UUID userId, UUID deviceId);

    boolean existsByUserIdAndDeviceId(UUID userId, UUID deviceId);

    Optional<Device> findByDeviceToken(String deviceToken);

    List<Device> findAllByUserIdAndStatus(UUID userId, DeviceStatus status);
}
