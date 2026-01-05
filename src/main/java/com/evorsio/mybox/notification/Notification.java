package com.evorsio.mybox.notification;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_user_status", columnList = "user_id,status"),
        @Index(name = "idx_notification_created_at", columnList = "created_at"),
        @Index(name = "idx_notification_user_type", columnList = "user_id,type"),
        @Index(name = "idx_notification_priority", columnList = "priority")
})
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.UNREAD;

    // 关联实体信息（可选，用于关联到具体业务对象）
    @Column(length = 50)
    private String entityType;  // "FILE", "DEVICE", "FOLDER", null

    private UUID entityId;

    // 操作链接（可选，用于点击通知后跳转）
    @Column(length = 500)
    private String actionUrl;

    // 是否允许删除（系统通知可能不允许删除）
    @Column(nullable = false)
    private Boolean deletable = true;

    // 过期时间（可选，用于自动清理过期通知）
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (priority == null) {
            priority = NotificationPriority.NORMAL;
        }
        if (status == null) {
            status = NotificationStatus.UNREAD;
        }
        if (deletable == null) {
            deletable = true;
        }
    }
}

