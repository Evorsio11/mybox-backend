package com.evorsio.mybox.device.domain;

/**
 * 设备权限枚举
 * 定义设备在云盘系统中的权限类型
 */
public enum DevicePermission {
    READ,       // 可以下载文件
    WRITE,      // 可以上传文件
    SYNC        // 可以同步文件
}
