package com.evorsio.mybox.file.service;

import com.evorsio.mybox.file.properties.FileUploadProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkUploadConcurrencyManager {
    private final FileUploadProperties fileUploadProperties;
    private Semaphore semaphore;
    private final ConcurrentHashMap<UUID, AtomicInteger> activeUploads = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.semaphore = new Semaphore(fileUploadProperties.getChunk().getMaxConcurrent());
        log.info("并发管理器初始化成功: maxConcurrent={}",
                fileUploadProperties.getChunk().getMaxConcurrent());
    }

    /**
     * 尝试获取并发上传许可
     * @param uploadId 上传会话ID
     * @return 是否成功获取许可
     */
    public boolean tryAcquire(UUID uploadId) {
        if (!fileUploadProperties.getChunk().isEnableConcurrent()) {
            return true; // 如果未启用并发控制，直接返回true
        }

        try {
            if (!semaphore.tryAcquire()) {
                log.warn("并发上传限制已达到: uploadId={}", uploadId);
                return false;
            }
            activeUploads.computeIfAbsent(uploadId, k -> new AtomicInteger(0)).incrementAndGet();
            log.debug("获取并发上传许可成功: uploadId={}, 活跃上传数={}",
                    uploadId, activeUploads.get(uploadId).get());
            return true;
        } catch (Exception e) {
            log.error("获取并发上传许可失败: uploadId={}", uploadId, e);
            return false;
        }
    }

    /**
     * 释放并发上传许可
     * @param uploadId 上传会话ID
     */
    public void release(UUID uploadId) {
        if (!fileUploadProperties.getChunk().isEnableConcurrent()) {
            return;
        }

        try {
            semaphore.release();
            AtomicInteger count = activeUploads.get(uploadId);
            if (count != null && count.decrementAndGet() == 0) {
                activeUploads.remove(uploadId);
            }
            log.debug("释放并发上传许可成功: uploadId={}", uploadId);
        } catch (Exception e) {
            log.error("释放并发上传许可失败: uploadId={}", uploadId, e);
        }
    }

    /**
     * 获取当前活跃的上传数
     * @param uploadId 上传会话ID
     * @return 活跃上传数
     */
    public int getActiveUploadCount(UUID uploadId) {
        AtomicInteger count = activeUploads.get(uploadId);
        return count != null ? count.get() : 0;
    }
}
