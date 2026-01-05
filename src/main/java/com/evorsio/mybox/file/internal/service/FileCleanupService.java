package com.evorsio.mybox.file.internal.service;

import com.evorsio.mybox.file.FileConfigService;
import com.evorsio.mybox.file.FileRecord;
import com.evorsio.mybox.file.FileService;
import com.evorsio.mybox.file.FileStatus;
import com.evorsio.mybox.file.MinioStorageService;
import com.evorsio.mybox.file.internal.properties.FileStorageProperties;
import com.evorsio.mybox.file.internal.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件清理服务
 * <p>
 * 定期清理超期的已删除文件记录，并减少文件引用计数
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileCleanupService {

    private final FileRecordRepository fileRecordRepository;
    private final MinioStorageService minioStorageService;
    private final FileStorageProperties fileStorageProperties;
    private final FileConfigService fileConfigService;
    private final FileService fileService;

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void purgeExpiredFileRecords() {
        // 优先使用运行时配置（小时），否则使用配置文件的默认值
        int retentionHours = fileConfigService.getRetentionHours();
        LocalDateTime expireTime = LocalDateTime.now().minusHours(retentionHours);
        List<FileRecord> fileRecordsToPurge = fileRecordRepository.findByStatusAndDeletedAtBefore(FileStatus.DELETED, expireTime);

        log.info("开始清理 {} 个超期文件记录（保留时间：{}小时）", fileRecordsToPurge.size(), retentionHours);

        for (FileRecord fileRecord : fileRecordsToPurge) {
            try {
                // 减少文件的引用计数
                int newRefCount = fileService.decrementReferenceCount(fileRecord.getFileId());
                log.info("文件引用计数减少: fileId={}, newRefCount={}", fileRecord.getFileId(), newRefCount);

                // 如果引用计数降为0，尝试删除实际文件
                if (newRefCount == 0) {
                    fileService.tryDeleteFile(fileRecord.getFileId());
                }

                // 更新文件记录状态为 PURGED
                fileRecord.setStatus(FileStatus.PURGED);
                fileRecordRepository.save(fileRecord);

                log.info("文件记录已物理删除并标记为PURGED: fileRecordId={}, ownerId={}", fileRecord.getId(), fileRecord.getOwnerId());
            } catch (Exception e) {
                log.error("物理删除失败: fileRecordId={}, error={}", fileRecord.getId(), e.getMessage(), e);
            }
        }

        log.info("超期文件记录清理完成");
    }
}
