package com.evorsio.mybox.device.internal.service.impl;

import com.evorsio.mybox.auth.UserLoggedInEvent;
import com.evorsio.mybox.auth.UserRegisteredEvent;
import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.device.Device;
import com.evorsio.mybox.device.DeviceType;
import com.evorsio.mybox.device.internal.exception.DeviceException;
import com.evorsio.mybox.device.internal.repository.DeviceRepository;
import com.evorsio.mybox.device.internal.util.DeviceUtil;
import com.evorsio.mybox.device.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    @Override
    public void registerDevice(UserRegisteredEvent event) {
        Device device = deviceRepository.findByUserIdAndDeviceId(event.getUserId(), event.getDeviceId())
                .orElse(null);

        String fingerprint = generateFingerprintFromEvent(event);

        if (device != null) {
            // 更新已存在设备指纹
            device.setFingerprint(fingerprint);
            device.setOsName(event.getOsName());
            device.setOsVersion(event.getOsVersion());

            deviceRepository.save(device);
            log.info("注册阶段：更新已存在设备指纹: userId={}, deviceId={}, fingerprint={}",
                    event.getUserId(), event.getDeviceId(), fingerprint);
        } else {
            // 创建新设备（不生成 deviceToken）
            device = buildDeviceFromEvent(event, fingerprint, null);
            deviceRepository.save(device);
            log.info("注册阶段：创建新设备（无令牌）: userId={}, deviceId={}, fingerprint={}",
                    event.getUserId(), event.getDeviceId(), fingerprint);
        }
    }

    @Override
    public String loginDeviceAndReturnToken(UserLoggedInEvent event) {
        Device device = deviceRepository.findByUserIdAndDeviceId(event.getUserId(), event.getDeviceId())
                .orElse(null);

        String fingerprint = generateFingerprintFromEvent(event);
        LocalDateTime now = LocalDateTime.now();
        String deviceToken;

        if (device != null) {
            // 验证设备指纹
            if (!fingerprint.equals(device.getFingerprint())) {
                log.warn("设备指纹不匹配，拒绝登录: deviceId={}, user={}, expected={}, actual={}",
                        event.getDeviceId(), event.getUserId(), device.getFingerprint(), fingerprint);
                throw new DeviceException(ErrorCode.DEVICE_FINGERPRINT_MISMATCH);
            }

            // 指纹验证通过，生成令牌（如果没有）
            if (device.getDeviceToken() == null || device.getDeviceToken().isEmpty()) {
                deviceToken = DeviceUtil.generateDeviceToken();
                device.setDeviceToken(deviceToken);
                log.info("登录阶段：生成设备令牌: deviceId={}, token={}, user={}",
                        event.getDeviceId(), deviceToken, event.getUserId());
            } else {
                deviceToken = device.getDeviceToken();
            }

            device.setOsName(event.getOsName());
            device.setOsVersion(event.getOsVersion());
            device.setLastActiveAt(now);

            deviceRepository.save(device);
            log.info("登录阶段：更新设备信息: userId={}, deviceId={}", event.getUserId(), event.getDeviceId());

        } else {
            // 新设备登录
            deviceToken = DeviceUtil.generateDeviceToken();
            device = buildDeviceFromEvent(event, fingerprint, deviceToken);
            device.setLastActiveAt(now);
            deviceRepository.save(device);
            log.info("登录阶段：创建新设备并生成令牌: deviceId={}, token={}, user={}",
                    event.getDeviceId(), deviceToken, event.getUserId());
        }

        return deviceToken; // 只返回 deviceToken
    }


    /**
     * 根据事件生成设备指纹
     */
    private String generateFingerprintFromEvent(Object event) {
        if (event instanceof UserRegisteredEvent reg) {
            return DeviceUtil.generateFingerprint(
                    reg.getDeviceId().toString(),
                    reg.getDeviceName(),
                    reg.getDeviceType(),
                    reg.getOsName(),
                    reg.getOsVersion()
            );
        } else if (event instanceof UserLoggedInEvent login) {
            return DeviceUtil.generateFingerprint(
                    login.getDeviceId().toString(),
                    login.getDeviceName(),
                    login.getDeviceType(),
                    login.getOsName(),
                    login.getOsVersion()
            );
        }
        throw new IllegalArgumentException("不支持的事件类型：" + event.getClass());
    }

    /**
     * 构建设备对象
     *
     * @param event        注册或登录事件
     * @param fingerprint  指纹
     * @param deviceToken  设备令牌，可为 null（注册阶段）
     */
    private Device buildDeviceFromEvent(Object event, String fingerprint, String deviceToken) {
        Device device = new Device();
        if (event instanceof UserRegisteredEvent reg) {
            device.setDeviceId(reg.getDeviceId());
            device.setUserId(reg.getUserId());
            device.setDeviceName(reg.getDeviceName());
            device.setDeviceType(DeviceType.valueOf(reg.getDeviceType()));
            device.setOsName(reg.getOsName());
            device.setOsVersion(reg.getOsVersion());
        } else if (event instanceof UserLoggedInEvent login) {
            device.setDeviceId(login.getDeviceId());
            device.setUserId(login.getUserId());
            device.setDeviceName(login.getDeviceName());
            device.setDeviceType(DeviceType.valueOf(login.getDeviceType()));
            device.setOsName(login.getOsName());
            device.setOsVersion(login.getOsVersion());
        } else {
            throw new IllegalArgumentException("不支持的事件类型：" + event.getClass());
        }
        device.setFingerprint(fingerprint);
        device.setDeviceToken(deviceToken);
        return device;
    }
}
