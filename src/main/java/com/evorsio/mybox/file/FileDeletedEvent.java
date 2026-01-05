package com.evorsio.mybox.file;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件删除事件
 * <p>
 * 当文件被删除（软删除）时发布此事件
 * 监听者应清理相关的索引和缓存数据
 */
@Getter
public class FileDeletedEvent extends ApplicationEvent {

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
     * 文件所属文件夹ID（可能为null）
     */
    private final UUID folderId;

    /**
     * 删除时间
     */
    private final LocalDateTime deletedAt;

    /**
     * 是否永久删除（物理删除）
     */
    private final boolean permanent;

    public FileDeletedEvent(Object source, UUID fileRecordId, UUID ownerId,
                           String fileName, UUID folderId,
                           LocalDateTime deletedAt, boolean permanent) {
        super(source);
        this.fileRecordId = fileRecordId;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.folderId = folderId;
        this.deletedAt = deletedAt;
        this.permanent = permanent;
    }
}
