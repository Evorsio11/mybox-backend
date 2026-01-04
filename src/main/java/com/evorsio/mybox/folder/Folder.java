package com.evorsio.mybox.folder;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "folders", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "primary_device_id", "parent_folder_id", "folder_name"})
})
@Comment("文件夹逻辑组织结构")
public class Folder {

    // ========== 主键和业务标识 ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("文件夹业务唯一标识（主键）")
    private int id;

    @Column(nullable = false, unique = true)
    @Comment("设备唯一标识（UUID，客户端生成）")
    private UUID folderId;

    // ========== 所属关系 ==========
    @Column(nullable = false)
    @Comment("所属用户 ID")
    private UUID userId;

    @Column(nullable = false)
    @Comment("所属主设备 ID（文件夹逻辑上归属的主设备）")
    private UUID primaryDeviceId;

    // ========== 层级结构 ==========
    @Column(nullable = false)
    @Comment("文件夹名称")
    private String folderName;

    @Column
    @Comment("父文件夹 ID，根目录为 null")
    private UUID parentFolderId;

    @Column(nullable = false)
    @Comment("逻辑完整路径，如 /Documents/Work/Project")
    private String fullPath;

    @Column(nullable = false)
    @Comment("层级深度，根目录为 0")
    private Integer level;

    // ========== 状态 ==========
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("文件夹状态：ACTIVE / DELETED / ARCHIVED")
    private FolderStatus status = FolderStatus.ACTIVE;

    // ========== 元数据 ==========
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("文件夹类型")
    private FolderType folderType;

    @Column(length = 20)
    @Comment("文件夹颜色标签（十六进制颜色码）")
    private String color;

    @Column(nullable = false)
    @Comment("是否收藏")
    private Boolean isStarred;

    @Column
    @Comment("图标名称")
    private String iconName;

    @Column
    @Comment("文件夹描述")
    private String description;

    // ========== 统计信息 ==========
    @Column
    @Comment("直接包含的文件数量")
    private Integer fileCount;

    @Column
    @Comment("直接包含的子文件夹数量")
    private Integer folderCount;

    @Column
    @Comment("所有子孙文件总数（递归）")
    private Integer totalFileCount;

    @Column
    @Comment("文件夹总大小（字节，包含所有子孙文件）")
    private Long totalSize;

    // ========== 排序 ==========
    @Column
    @Comment("自定义排序序号")
    private Integer sortOrder;

    // ========== 审计字段 ==========
    @Column(nullable = false, updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column
    @Comment("删除时间（软删除）")
    private LocalDateTime deletedAt;

    // ========== 生命周期回调 ==========
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // 设置默认值
        if (level == null) {
            level = 0;
        }
        if (fullPath == null) {
            fullPath = "";
        }
        if (fileCount == null) fileCount = 0;
        if (folderCount == null) folderCount = 0;
        if (totalFileCount == null) totalFileCount = 0;
        if (totalSize == null) totalSize = 0L;
        if (isStarred == null) isStarred = false;
        if (sortOrder == null) sortOrder = 0;
        if (folderType == null) folderType = FolderType.USER;
        if (status == null) status = FolderStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== 业务方法 ==========

    /**
     * 是否是根文件夹
     */
    public boolean isRoot() {
        return parentFolderId == null;
    }

    /**
     * 是否是系统文件夹
     */
    public boolean isSystemFolder() {
        return folderType == FolderType.SYSTEM;
    }

    /**
     * 判断是否已删除
     */
    public boolean isDeleted() {
        return status == FolderStatus.DELETED;
    }

    /**
     * 判断是否活跃
     */
    public boolean isActive() {
        return status == FolderStatus.ACTIVE;
    }

    /**
     * 判断是否已归档
     */
    public boolean isArchived() {
        return status == FolderStatus.ARCHIVED;
    }

    /**
     * 软删除文件夹
     */
    public void markAsDeleted() {
        this.status = FolderStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 恢复文件夹
     */
    public void restore() {
        this.status = FolderStatus.ACTIVE;
        this.deletedAt = null;
    }

    /**
     * 归档文件夹
     */
    public void archive() {
        this.status = FolderStatus.ARCHIVED;
    }

    /**
     * 检查是否可以删除（非系统文件夹且为空）
     */
    public boolean canDelete() {
        return !isSystemFolder() &&
                (fileCount == null || fileCount == 0) &&
                (folderCount == null || folderCount == 0);
    }

    /**
     * 检查是否为空文件夹
     */
    public boolean isEmpty() {
        return (fileCount == null || fileCount == 0) &&
               (folderCount == null || folderCount == 0);
    }

    /**
     * 获取存储使用率
     */
    public double getStorageUsagePercentage(long quota) {
        if (quota <= 0) return 0.0;
        return (totalSize != null ? totalSize : 0) * 100.0 / quota;
    }
}
