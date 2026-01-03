package com.evorsio.mybox.device;

public enum DeviceStatus {
    /**
     * 活跃（正常使用）
     */
    ACTIVE,

    /**
     * 已禁用（暂时停用）
     */
    DISABLED,

    /**
     * 已封禁（异常或安全原因）
     */
    BLOCKED,

    /**
     * 已删除（软删除）
     */
    DELETED
}
