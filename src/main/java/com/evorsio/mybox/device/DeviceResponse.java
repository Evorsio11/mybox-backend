package com.evorsio.mybox.device;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
public class DeviceResponse {
    // ========== 基本信息 ==========
    private String id;                     // 设备ID (UUID)
    private String deviceName;             // 设备名称
    private DeviceType deviceType;         // 设备类型 (DESKTOP/MOBILE/TABLET)
    private String osName;                 // 操作系统
    private String osVersion;              // 系统版本

    // ========== 在线状态 ==========
    private OnlineStatus onlineStatus;     // 在线状态 (ONLINE/OFFLINE)
    private LocalDateTime lastActiveAt;    // 最后活跃时间

    // ========== 存储信息 ==========
    private Long storageQuota;             // 存储配额（字节，0表示不限制）
    private Long storageUsed;              // 已使用存储（字节）
    private Boolean isPrimary;             // 是否为主设备

    // ========== 同步状态 ==========
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SyncStatus syncStatus;         // 同步状态 (SYNCED/SYNCING/PAUSED/ERROR)
    private LocalDateTime lastSyncTime;    // 最后同步时间

    // ========== 权限信息 ==========
    private Set<DevicePermission> permissions;  // 设备权限 (READ/WRITE/SYNC)

    // ========== 设备状态 ==========
    private DeviceStatus status;           // 设备状态 (ACTIVE/DISABLED/BLOCKED)

    // ========== 时间信息 ==========
    private LocalDateTime createdAt;       // 设备首次激活时间
}
