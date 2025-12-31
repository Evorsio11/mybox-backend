package com.evorsio.mybox.device.domain;

/**
 * 设备角色枚举
 * 定义设备在云盘系统中的权限级别
 */
public enum DeviceRole {
    /**
     * 主设备
     * 拥有完全控制权，可以删除其他设备的文件，管理所有设备
     */
    PRIMARY,

    /**
     * 普通设备
     * 可以上传/下载/管理自己的文件，不能删除其他设备的文件
     */
    SECONDARY,

    /**
     * 只读设备
     * 只能下载文件，不能上传、修改或删除文件
     */
    READ_ONLY
}
