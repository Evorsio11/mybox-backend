package com.evorsio.mybox.folder;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件夹响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderResponse {

    // ========== 基本信息 ==========
    /**
     * 文件夹业务 ID（UUID）
     */
    private UUID folderId;

    /**
     * 文件夹名称
     */
    private String folderName;

    /**
     * 父文件夹业务 ID（根目录为 null）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID parentFolderId;

    /**
     * 逻辑完整路径，如 /Documents/Work/Project
     */
    private String fullPath;

    /**
     * 层级深度，根目录为 0
     */
    private Integer level;

    // ========== 状态信息 ==========
    /**
     * 文件夹状态（ACTIVE/DELETED/ARCHIVED）
     */
    private FolderStatus status;

    /**
     * 文件夹类型（SYSTEM/USER）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FolderType folderType;

    // ========== 元数据 ==========
    /**
     * 文件夹颜色标签（十六进制颜色码）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String color;

    /**
     * 是否收藏
     */
    private Boolean isStarred;

    /**
     * 图标名称
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String iconName;

    /**
     * 文件夹描述
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;

    // ========== 统计信息 ==========
    /**
     * 直接包含的文件数量
     */
    private Integer fileCount;

    /**
     * 直接包含的子文件夹数量
     */
    private Integer folderCount;

    /**
     * 所有子孙文件总数（递归）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalFileCount;

    /**
     * 文件夹总大小（字节，包含所有子孙文件）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long totalSize;

    // ========== 排序信息 ==========
    /**
     * 自定义排序序号
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer sortOrder;

    // ========== 设备信息 ==========
    /**
     * 所属主设备 ID（设备的 deviceId，UUID）
     */
    private UUID primaryDeviceId;

    // ========== 时间信息 ==========
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ========== 扩展信息（用于懒加载） ==========
    /**
     * 是否有子文件夹（用于前端显示展开图标）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean hasChildren;

    /**
     * 是否是根文件夹
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isRoot;
}
