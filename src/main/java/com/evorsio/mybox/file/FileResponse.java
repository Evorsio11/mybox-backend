package com.evorsio.mybox.file;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件记录响应 DTO
 * <p>
 * 用于 API 响应，隐藏内部存储实现细节
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    /**
     * 文件记录 ID
     */
    private UUID id;

    /**
     * 所属文件夹 ID（null 表示未分类）
     */
    private UUID folderId;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件 MIME 类型
     */
    private String contentType;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 所属用户 ID
     */
    private UUID ownerId;

    /**
     * 文件状态
     */
    private FileStatus status;

    /**
     * 自定义元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间（软删除时间）
     */
    private LocalDateTime deletedAt;

    /**
     * 是否为共享存储（与其他文件记录共享同一文件）
     */
    private Boolean sharedStorage;

    /**
     * 从 FileRecord 实体转换为 DTO
     */
    public static FileResponse fromEntity(FileRecord fileRecord) {
        return FileResponse.builder()
                .id(fileRecord.getId())
                .folderId(fileRecord.getFolderId())
                .originalFileName(fileRecord.getOriginalFileName())
                .contentType(fileRecord.getContentType())
                .size(fileRecord.getSize())
                .fileHash(fileRecord.getFileHash())
                .ownerId(fileRecord.getOwnerId())
                .status(fileRecord.getStatus())
                .metadata(fileRecord.getMetadata())
                .createdAt(fileRecord.getCreatedAt())
                .updatedAt(fileRecord.getUpdatedAt())
                .deletedAt(fileRecord.getDeletedAt())
                .sharedStorage(fileRecord.isSharedStorage())
                .build();
    }
}
