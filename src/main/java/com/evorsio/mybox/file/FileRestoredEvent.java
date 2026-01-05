package com.evorsio.mybox.file;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件恢复事件
 * <p>
 * 当已删除的文件被恢复时发布此事件
 * 监听者可以重建索引和缓存数据
 */
@Getter
public class FileRestoredEvent extends ApplicationEvent {

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
     * 恢复时间
     */
    private final LocalDateTime restoredAt;

    public FileRestoredEvent(Object source, UUID fileRecordId, UUID ownerId,
                            String fileName, UUID folderId,
                            LocalDateTime restoredAt) {
        super(source);
        this.fileRecordId = fileRecordId;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.folderId = folderId;
        this.restoredAt = restoredAt;
    }
}
