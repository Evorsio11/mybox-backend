package com.evorsio.mybox.device.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public class DeviceUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits

    /**
     * 生成设备指纹
     * @param deviceId 设备UUID或标识
     * @param deviceName 设备名称
     * @param deviceType 设备类型
     * @param osName 操作系统名称
     * @param osVersion 操作系统版本
     * @return SHA-256 Hex编码字符串
     */
    public static String generateFingerprint(String deviceId, String deviceName, String deviceType, String osName, String osVersion) {
        try {
            String data = String.format("%s|%s|%s|%s|%s",
                    deviceId, deviceName, deviceType, osName, osVersion);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // 转Hex字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String fingerprint = hexString.toString();
            log.info("生成设备指纹成功: deviceId={}, fingerprint={}", deviceId, fingerprint);
            return fingerprint;

        } catch (Exception e) {
            log.error("生成设备指纹失败: deviceId={}, 错误信息: {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("生成设备指纹失败", e);
        }
    }

    /**
     * 生成设备认证Token
     * @return Base64 URL-safe编码Token
     */
    public static String generateDeviceToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        log.info("生成设备令牌成功: token={}", token);
        return token;
    }
}
