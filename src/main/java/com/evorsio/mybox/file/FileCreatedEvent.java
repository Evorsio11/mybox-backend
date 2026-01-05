package com.evorsio.mybox.file;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件创建事件
 * <p>
 * 当新文件上传成功时发布此事件
 * 其他模块（如搜索模块、统计模块）可以监听此事件执行相应操作
 */
@Getter
public class FileCreatedEvent extends ApplicationEvent {

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
     * 文件存储ID（File实体ID）
     */
    private final UUID storageId;

    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;

    public FileCreatedEvent(Object source, UUID fileRecordId, UUID ownerId,
                           String fileName, String contentType, Long fileSize,
                           UUID folderId, UUID storageId, LocalDateTime createdAt) {
        super(source);
        this.fileRecordId = fileRecordId;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.folderId = folderId;
        this.storageId = storageId;
        this.createdAt = createdAt;
    }
}
