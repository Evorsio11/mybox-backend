package com.evorsio.mybox.notification;

public enum NotificationPriority {
    LOW,       // 低优先级（如功能更新、一般提示）
    NORMAL,    // 普通优先级（如文件上传完成、设备上线）
    HIGH,      // 高优先级（如存储警告、设备离线）
    URGENT     // 紧急（如安全警报、多次登录失败）
}
