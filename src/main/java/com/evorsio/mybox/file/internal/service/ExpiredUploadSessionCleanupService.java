package com.evorsio.mybox.file.service;

import com.evorsio.mybox.file.FileChunk;
import com.evorsio.mybox.file.MinioStorageService;
import com.evorsio.mybox.file.UploadSession;
import com.evorsio.mybox.file.UploadStatus;
import com.evorsio.mybox.file.internal.properties.FileUploadProperties;
import com.evorsio.mybox.file.internal.repository.FileChunkRepository;
import com.evorsio.mybox.file.internal.repository.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiredUploadSessionCleanupService {
    private final UploadSessionRepository uploadSessionRepository;
    private final FileChunkRepository fileChunkRepository;
    private final MinioStorageService minioStorageService;
    private final FileUploadProperties fileUploadProperties;

    /**
     * 定时清理过期的上传会话
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredSessions() {
        if (!fileUploadProperties.getChunk().isCleanupExpired()) {
            log.debug("过期会话清理功能未启用");
            return;
        }

        try {
            // 解析 sessionTimeout 配置（如 "24h"）
            int sessionTimeoutHours = parseTime(fileUploadProperties.getChunk().getSessionTimeout());
            LocalDateTime expireTime = LocalDateTime.now().minusHours(sessionTimeoutHours);

            // 查找所有过期的会话（状态为 UPLOADING 或 INIT，且创建时间超过阈值）
            List<UploadSession> expiredSessions = uploadSessionRepository
                    .findByStatusAndCreatedAtBefore(UploadStatus.UPLOADING, expireTime);

            // 同时查找过期的 INIT 状态会话
            List<UploadSession> expiredInitSessions = uploadSessionRepository
                    .findByStatusAndCreatedAtBefore(UploadStatus.INIT, expireTime);

            expiredSessions.addAll(expiredInitSessions);

            if (expiredSessions.isEmpty()) {
                log.debug("没有需要清理的过期会话");
                return;
            }

            log.info("开始清理过期会话: count={}", expiredSessions.size());

            for (UploadSession session : expiredSessions) {
                try {
                    cleanupSession(session);
                } catch (Exception e) {
                    log.error("清理过期会话失败: sessionId={}", session.getId(), e);
                }
            }

            log.info("清理过期会话完成: total={}", expiredSessions.size());

        } catch (Exception e) {
            log.error("清理过期会话任务失败", e);
        }
    }

    /**
     * 解析时间字符串（如 "24h", "5s"）为小时数
     */
    private int parseTime(String time) {
        if (time == null || time.isEmpty()) {
            return 24; // 默认24小时
        }
        time = time.trim().toLowerCase();
        try {
            if (time.endsWith("h")) {
                return Integer.parseInt(time.substring(0, time.length() - 1));
            } else if (time.endsWith("s")) {
                return Integer.parseInt(time.substring(0, time.length() - 1)) / 3600;
            } else if (time.endsWith("m")) {
                return Integer.parseInt(time.substring(0, time.length() - 1)) / 60;
            } else {
                return Integer.parseInt(time);
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析时间配置: {}, 使用默认值24小时", time);
            return 24;
        }
    }

    /**
     * 清理单个会话
     */
    private void cleanupSession(UploadSession session) {
        log.info("清理过期会话: sessionId={}, ownerId={}, fileName={}",
                session.getId(), session.getOwnerId(), session.getOriginalFileName());

        try {
            // 1. 获取所有分片
            List<FileChunk> chunks = fileChunkRepository.findByUploadSessionId(session.getId());

            if (!chunks.isEmpty()) {
                // 2. 删除 MinIO 中的临时分片
                List<String> chunkObjectNames = chunks.stream()
                        .map(FileChunk::getObjectName)
                        .collect(Collectors.toList());

                try {
                    minioStorageService.deleteChunks(session.getBucket(), chunkObjectNames);
                    log.debug("删除临时分片成功: sessionId={}, chunkCount={}",
                            session.getId(), chunks.size());
                } catch (Exception e) {
                    log.error("删除临时分片失败: sessionId={}", session.getId(), e);
                }

                // 3. 删除数据库中的分片记录
                fileChunkRepository.deleteByUploadSessionId(session.getId());
            }

            // 4. 更新会话状态为 EXPIRED
            session.setStatus(UploadStatus.EXPIRED);
            uploadSessionRepository.save(session);

            log.info("清理过期会话成功: sessionId={}", session.getId());

        } catch (Exception e) {
            log.error("清理过期会话异常: sessionId={}", session.getId(), e);
            throw e;
        }
    }

    /**
     * 手动触发清理（可选，用于测试或手动清理）
     */
    @Transactional
    public void manualCleanup() {
        log.info("手动触发清理过期会话");
        cleanupExpiredSessions();
    }
}
