package com.evorsio.mybox.notification;

public enum NotificationType {
    // ==================== 安全通知 ====================
    PASSWORD_RESET_REQUEST,      // 密码重置请求
    PASSWORD_RESET_SUCCESS,      // 密码重置成功
    NEW_DEVICE_LOGIN,            // 新设备登录
    MULTIPLE_LOGIN_FAILURES,     // 多次登录失败
    ACCOUNT_LOCKED,              // 账号被锁定
    DEVICE_OFFLINE,              // 设备离线提醒

    // ==================== 文件管理通知 ====================
    FILE_UPLOAD_COMPLETE,        // 文件上传完成
    FILE_UPLOAD_FAILED,          // 文件上传失败
    BATCH_UPLOAD_COMPLETE,       // 批量上传完成
    FILE_DELETED,                // 文件删除通知
    FILE_RESTORED,               // 文件恢复成功
    STORAGE_WARNING,             // 存储空间不足警告
    STORAGE_FULL,                // 存储空间已满
    FILE_QUOTA_EXCEEDED,         // 文件配额超限

    // ==================== 设备管理通知 ====================
    DEVICE_ONLINE,               // 设备上线
    DEVICE_SYNC_COMPLETE,        // 设备同步完成
    DEVICE_SYNC_FAILED,          // 设备同步失败
    DEVICE_QUOTA_WARNING,        // 设备配额警告
    DEVICE_REMOVED,              // 设备被移除
    UNAUTHORIZED_DEVICE_ACCESS,  // 未授权设备访问

    // ==================== 系统通知 ====================
    SYSTEM_MAINTENANCE,          // 系统维护通知
    FEATURE_UPDATE,              // 功能更新通知
    SECURITY_ALERT,              // 安全警报
    SYSTEM_ANNOUNCEMENT          // 系统公告
}
