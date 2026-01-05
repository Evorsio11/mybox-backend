package com.evorsio.mybox.file;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件更新事件
 * <p>
 * 当文件元数据更新时发布此事件
 * 注意：文件内容更新（重新上传）会触发此事件
 */
@Getter
public class FileUpdatedEvent extends ApplicationEvent {

    /**
     * 文件记录ID
     */
    private final UUID fileRecordId;

    /**
     * 文件所有者ID
     */
    private final UUID ownerId;

    /**
     * 文件名称
     */
    private final String fileName;

    /**
     * 文件MIME类型
     */
    private final String contentType;

    /**
     * 文件大小（字节）
     */
    private final Long fileSize;

    /**
     * 文件所属文件夹ID（可能为null）
     */
    private final UUID folderId;

    /**
     * 文件存储ID（File实体ID，如果内容变化则可能是新的ID）
     */
    private final UUID storageId;

    /**
     * 更新时间
     */
    private final LocalDateTime updatedAt;

    /**
     * 内容是否变化（true表示文件内容被重新上传）
     */
    private final boolean contentChanged;

    public FileUpdatedEvent(Object source, UUID fileRecordId, UUID ownerId,
                           String fileName, String contentType, Long fileSize,
                           UUID folderId, UUID storageId, LocalDateTime updatedAt,
                           boolean contentChanged) {
        super(source);
        this.fileRecordId = fileRecordId;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.folderId = folderId;
        this.storageId = storageId;
        this.updatedAt = updatedAt;
        this.contentChanged = contentChanged;
    }
}
