package com.evorsio.mybox.device.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.evorsio.mybox.device.Device;
import com.evorsio.mybox.device.DeviceStatus;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {
    Optional<Device> findByUserIdAndDeviceId(UUID userId, UUID deviceId);

    boolean existsByUserIdAndDeviceId(UUID userId, UUID deviceId);

    Optional<Device> findByDeviceToken(String deviceToken);
    
    Optional<Device> findByUserIdAndFingerprint(UUID userId, String fingerprint);

    List<Device> findAllByUserIdAndStatus(UUID userId, DeviceStatus status);

    Optional<Device> findByUserIdAndIsPrimaryTrueAndStatus(UUID userId, DeviceStatus status);
}
