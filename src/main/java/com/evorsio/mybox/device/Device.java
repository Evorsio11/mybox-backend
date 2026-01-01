package com.evorsio.mybox.device;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "devices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "device_name"}),
        @UniqueConstraint(columnNames = "device_uuid")
})
@Comment("用户设备信息表")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("设备记录唯一标识")
    private int id;

    @Column(nullable = false)
    @Comment("所属用户id")
    private UUID userId;

    @Column(nullable = false, unique = true)
    @Comment("设备唯一标识（UUID，客户端生成）")
    private UUID deviceId;

    @Column(nullable = false)
    @Comment("设备名称")
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("设备类型")
    private DeviceType deviceType;

    @Column
    @Comment("操作系统")
    private String osName;

    @Column
    @Comment("操作系统版本")
    private String osVersion;

    @Column
    @Comment("最后心跳时间")
    private LocalDateTime lastHeartbeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("在线状态")
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    // ========== MinIO 存储配置 ==========

    @Column
    @Comment("设备的默认存储桶（Bucket）名称")
    private String defaultBucket;

    @Column
    @Comment("设备存储配额（字节，0表示不限制）")
    private Long storageQuota;

    @Column
    @Comment("已使用存储空间（字节）")
    private Long storageUsed;

    @Column(nullable = false)
    @Comment("是否为主存储设备")
    private Boolean isPrimary = false;

    // ========== 在线状态 ==========

    @Column
    @Comment("最后活跃时间")
    private LocalDateTime lastActiveAt;

    // ========== 同步状态 ==========

    @Enumerated(EnumType.STRING)
    @Comment("同步状态：SYNCED / SYNCING / PAUSED / ERROR")
    private SyncStatus syncStatus = SyncStatus.SYNCED;

    @Column
    @Comment("最后同步时间")
    private LocalDateTime lastSyncTime;

    // ========== 设备角色与权限 ==========

    @Enumerated(EnumType.STRING)
    @Comment("设备权限：READ/WRITE/SYNC")
    private Set<DevicePermission> permissions = new HashSet<>(Set.of(
            DevicePermission.READ,
            DevicePermission.WRITE
    ));

    @Column(nullable = false)
    @Comment("是否允许同步文件到此设备")
    private Boolean allowSync = true;

    @Column(nullable = false)
    @Comment("是否允许从此设备上传文件")
    private Boolean allowUpload = true;

    // ========== 安全认证 ==========

    @Column(unique = true)
    @Comment("设备认证令牌")
    private String deviceToken;

    @Column(length = 255)
    @Comment("设备指纹")
    private String fingerprint;

    // ========== 设备状态 ==========

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("设备状态：ACTIVE / DISABLED / BLOCKED")
    private DeviceStatus status = DeviceStatus.ACTIVE;

    // ========== 时间戳 ==========

    @Column(nullable = false, updatable = false)
    @Comment("设备首次激活时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("设备信息最后更新时间")
    private LocalDateTime updatedAt;

    @Column
    @Comment("设备删除时间（软删除）")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = LocalDateTime.now();
    }

    // ========== 业务方法 ==========

    /**
     * 检查设备是否在线（5分钟内有心跳）
     */
    public OnlineStatus getOnlineStatus() {
        if (status != DeviceStatus.ACTIVE) return OnlineStatus.OFFLINE;
        if (lastHeartbeat == null) return OnlineStatus.OFFLINE;
        if (lastHeartbeat.isAfter(LocalDateTime.now().minusMinutes(1))) return OnlineStatus.ONLINE;
        if (lastHeartbeat.isAfter(LocalDateTime.now().minusMinutes(5))) return OnlineStatus.SLEEPING;
        return OnlineStatus.OFFLINE;
    }

    /**
     * 检查存储空间是否超限
     */
    public boolean isStorageExceeded(){
        return storageQuota != null &&
               storageQuota > 0 &&
               storageUsed != null &&
               storageUsed > storageQuota;
    }

    /**
     * 检查设备是否可以上传文件
     */
    public boolean canUpload(){
        return status == DeviceStatus.ACTIVE &&
               allowUpload &&
               !isStorageExceeded();
    }

    /**
     * 获取存储使用率（百分比）
     */
    public double getStorageUsagePercentage(){
        if(storageQuota == null || storageQuota == 0){
            return 0.0;
        }
        return (storageUsed != null ? storageUsed : 0) * 100.0 / storageQuota;
    }

    /**
     * 更新心跳（定时任务调用）
     */
    public void updateHeartbeat(){
        this.lastHeartbeat = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.onlineStatus = OnlineStatus.ONLINE;
    }
}
