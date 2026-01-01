package com.evorsio.mybox.device;

public enum OnlineStatus {
    ONLINE,     // 在线（可访问）
    OFFLINE,    // 离线（不可访问）
    SLEEPING    // 休眠（在线但低功耗，唤醒需要时间）
}
