package com.evorsio.mybox.device.domain;

public enum DeviceStatus {
    ACTIVE,     // 活跃（正常使用）
    DISABLED,   // 已禁用（暂时停用）
    BLOCKED     // 已封禁（异常或安全原因）
}
