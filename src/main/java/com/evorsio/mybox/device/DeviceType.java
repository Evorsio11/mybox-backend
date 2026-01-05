package com.evorsio.mybox.device;

public enum DeviceType {
    NAS,        // 网络存储设备
    DESKTOP,    // 台式机
    LAPTOP,     // 笔记本电脑
    MOBILE,     // 手机
    TABLET,     // 平板电脑
    SERVER,     // 服务器
    UNKNOWN;    // 未知设备

    /**
     * 安全的枚举转换方法
     * 如果传入的值无效，返回默认值 UNKNOWN
     *
     * @param value 设备类型字符串
     * @return 对应的枚举值，无效时返回 UNKNOWN
     */
    public static DeviceType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        try {
            return DeviceType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /**
     * 安全的枚举转换方法（指定默认值）
     *
     * @param value 设备类型字符串
     * @param defaultValue 默认值
     * @return 对应的枚举值，无效时返回默认值
     */
    public static DeviceType fromString(String value, DeviceType defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return DeviceType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
