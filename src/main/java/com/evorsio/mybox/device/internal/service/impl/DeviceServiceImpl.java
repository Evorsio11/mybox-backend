package com.evorsio.mybox.device.internal.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.evorsio.mybox.auth.DeviceInfoDto;
import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.device.Device;
import com.evorsio.mybox.device.DeviceLoginResponse;
import com.evorsio.mybox.device.DeviceResponse;
import com.evorsio.mybox.device.DeviceService;
import com.evorsio.mybox.device.DeviceStatus;
import com.evorsio.mybox.device.DeviceType;
import com.evorsio.mybox.device.internal.exception.DeviceException;
import com.evorsio.mybox.device.internal.mapper.DeviceMapper;
import com.evorsio.mybox.device.internal.repository.DeviceRepository;
import com.evorsio.mybox.device.internal.service.DeviceOnlineStatusService;
import com.evorsio.mybox.device.internal.util.DeviceUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;
    private final DeviceOnlineStatusService onlineStatusService;

    @Override
    public void registerDevice(UUID userId, DeviceInfoDto deviceInfo) {
        // 通过用户ID和设备名称查找已存在的设备
        Optional<Device> existingDevice = deviceRepository.findAllByUserIdAndStatus(userId, DeviceStatus.ACTIVE)
                .stream()
                .filter(d -> d.getDeviceName().equals(deviceInfo.getDeviceName()) 
                          && d.getDeviceType().name().equals(deviceInfo.getDeviceType()))
                .findFirst();

        if (existingDevice.isPresent()) {
            // 更新已存在设备
            Device device = existingDevice.get();
            String fingerprint = generateFingerprint(
                    device.getDeviceId().toString(),
                    deviceInfo.getDeviceName(),
                    deviceInfo.getDeviceType(),
                    deviceInfo.getOsName(),
                    deviceInfo.getOsVersion()
            );
            device.setFingerprint(fingerprint);
            device.setOsName(deviceInfo.getOsName());
            device.setOsVersion(deviceInfo.getOsVersion());

            deviceRepository.save(device);
            log.info("注册阶段：更新已存在设备: userId={}, deviceId={}, deviceName={}",
                    userId, device.getDeviceId(), deviceInfo.getDeviceName());
        } else {
            // 创建新设备，生成 deviceId
            UUID deviceId = UUID.randomUUID();
            String fingerprint = generateFingerprint(
                    deviceId.toString(),
                    deviceInfo.getDeviceName(),
                    deviceInfo.getDeviceType(),
                    deviceInfo.getOsName(),
                    deviceInfo.getOsVersion()
            );
            Device device = buildDevice(userId, deviceId, deviceInfo, fingerprint, null);
            deviceRepository.save(device);
            log.info("注册阶段：创建新设备: userId={}, deviceId={}, deviceName={}",
                    userId, deviceId, deviceInfo.getDeviceName());
        }
    }

    @Override
    public DeviceLoginResponse loginDeviceAndReturnToken(UUID userId, DeviceInfoDto deviceInfo) {
        // 通过设备名称和类型查找已存在的设备
        Optional<Device> existingDevice = deviceRepository.findAllByUserIdAndStatus(userId, DeviceStatus.ACTIVE)
                .stream()
                .filter(d -> d.getDeviceName().equals(deviceInfo.getDeviceName()) 
                          && d.getDeviceType().name().equals(deviceInfo.getDeviceType()))
                .findFirst();
        
        LocalDateTime now = LocalDateTime.now();
        String deviceToken;
        Device device;
        UUID deviceId;

        if (existingDevice.isPresent()) {
            // 设备已存在，验证指纹
            device = existingDevice.get();
            deviceId = device.getDeviceId();
            String fingerprint = generateFingerprint(
                    deviceId.toString(),
                    deviceInfo.getDeviceName(),
                    deviceInfo.getDeviceType(),
                    deviceInfo.getOsName(),
                    deviceInfo.getOsVersion()
            );
            
            // 验证指纹（允许OS版本更新）
            if (!fingerprint.equals(device.getFingerprint())) {
                log.info("设备指纹变化，更新指纹: deviceId={}, user={}",
                        deviceId, userId);
                device.setFingerprint(fingerprint);
            }

            // 生成或复用令牌
            if (device.getDeviceToken() == null || device.getDeviceToken().isEmpty()) {
                deviceToken = DeviceUtil.generateDeviceToken();
                device.setDeviceToken(deviceToken);
                log.info("登录阶段：生成设备令牌: deviceId={}, user={}",
                        deviceId, userId);
            } else {
                deviceToken = device.getDeviceToken();
            }

            device.setOsName(deviceInfo.getOsName());
            device.setOsVersion(deviceInfo.getOsVersion());
            device.setLastActiveAt(now);

            deviceRepository.save(device);
            log.info("登录阶段：更新设备信息: userId={}, deviceId={}, deviceName={}", 
                    userId, deviceId, deviceInfo.getDeviceName());

        } else {
            // 新设备登录，生成 deviceId 和 deviceToken
            deviceId = UUID.randomUUID();
            String fingerprint = generateFingerprint(
                    deviceId.toString(),
                    deviceInfo.getDeviceName(),
                    deviceInfo.getDeviceType(),
                    deviceInfo.getOsName(),
                    deviceInfo.getOsVersion()
            );
            deviceToken = DeviceUtil.generateDeviceToken();
            device = buildDevice(userId, deviceId, deviceInfo, fingerprint, deviceToken);
            device.setLastActiveAt(now);
            
            // 检查是否是用户的第一个设备，如果是则设为主设备
            List<Device> userDevices = deviceRepository.findAllByUserIdAndStatus(userId, DeviceStatus.ACTIVE);
            if (userDevices.isEmpty()) {
                device.setIsPrimary(true);
                log.info("登录阶段：创建用户的第一个设备并设为主设备: deviceId={}, deviceName={}, user={}",
                        deviceId, deviceInfo.getDeviceName(), userId);
            } else {
                log.info("登录阶段：创建新设备并生成令牌: deviceId={}, deviceName={}, user={}",
                        deviceId, deviceInfo.getDeviceName(), userId);
            }
            
            deviceRepository.save(device);
        }

        return new DeviceLoginResponse(deviceId, deviceToken);
    }

    @Override
    public List<DeviceResponse> listActiveDevices(UUID userId) {
        List<Device> devices = deviceRepository.findAllByUserIdAndStatus(userId, DeviceStatus.ACTIVE);
        return deviceMapper.toResponseList(devices, onlineStatusService);
    }

    @Override
    public void deleteDevice(UUID userId,UUID deviceId) {
        Device device = deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(()->new DeviceException(ErrorCode.DEVICE_NOT_FOUND));

        // 使用实体的业务方法标记删除（会同时设置 status 和 deletedAt）
        device.markAsDeleted();
        deviceRepository.save(device);

        // 删除 Redis 中的心跳记录
        onlineStatusService.removeHeartbeat(deviceId);

        log.info("设备删除成功: userId={}, deviceId={}", userId, deviceId);
    }

    @Override
    public void undoDeleteDevice(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(()->new DeviceException(ErrorCode.DEVICE_NOT_FOUND));

        // 使用实体的业务方法恢复（会同时设置 status 为 ACTIVE 和清空 deletedAt）
        device.restore();
        deviceRepository.save(device);

        log.info("设备恢复成功: userId={}, deviceId={}", userId, deviceId);
    }

    @Override
    public Device heartbeat(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new DeviceException(ErrorCode.DEVICE_NOT_FOUND));

        // 使用实体的业务方法判断状态
        if (!device.isActive()) {
            throw new DeviceException(ErrorCode.DEVICE_NOT_ACTIVE);
        }

        // 更新心跳时间到数据库
        device.setLastHeartbeat(LocalDateTime.now());
        device.setLastActiveAt(LocalDateTime.now());
        deviceRepository.save(device);

        // 记录心跳到 Redis
        onlineStatusService.recordHeartbeat(deviceId);

        log.info("设备心跳更新：userId={}, deviceId={}", userId, deviceId);

        return device;
    }

    @Override
    public UUID getPrimaryDeviceId(UUID userId) {
        return deviceRepository
                .findByUserIdAndIsPrimaryTrueAndStatus(userId, DeviceStatus.ACTIVE)
                .map(Device::getDeviceId)
                .orElse(null); // 如果没有主设备返回 null
    }

    /**
     * 生成设备指纹
     */
    private String generateFingerprint(String deviceId, String deviceName, String deviceType, 
                                      String osName, String osVersion) {
        return DeviceUtil.generateFingerprint(deviceId, deviceName, deviceType, osName, osVersion);
    }

    /**
     * 构建设备对象
     *
     * @param userId       用户ID
     * @param deviceId     设备ID
     * @param deviceInfo   设备信息
     * @param fingerprint  指纹
     * @param deviceToken  设备令牌，可为 null（注册阶段）
     */
    private Device buildDevice(UUID userId, UUID deviceId, DeviceInfoDto deviceInfo, 
                              String fingerprint, String deviceToken) {
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setUserId(userId);
        device.setDeviceName(deviceInfo.getDeviceName());
        device.setDeviceType(DeviceType.valueOf(deviceInfo.getDeviceType()));
        device.setOsName(deviceInfo.getOsName());
        device.setOsVersion(deviceInfo.getOsVersion());
        device.setFingerprint(fingerprint);
        device.setDeviceToken(deviceToken);
        return device;
    }
}
