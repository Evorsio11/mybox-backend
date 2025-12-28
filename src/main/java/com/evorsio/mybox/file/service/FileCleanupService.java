package com.evorsio.mybox.file.service;

import com.evorsio.mybox.file.domain.File;
import com.evorsio.mybox.file.domain.FileStatus;
import com.evorsio.mybox.file.properties.FileStorageProperties;
import com.evorsio.mybox.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileCleanupService {

    private final FileRepository fileRepository;
    private final MinioStorageService minioStorageService;
    private final FileStorageProperties fileStorageProperties;
    private final FileConfigService fileConfigService;

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void purgeExpiredFiles() {
        // 优先使用运行时配置（小时），否则使用配置文件的默认值
        int retentionHours = fileConfigService.getRetentionHours();
        LocalDateTime expireTime = LocalDateTime.now().minusHours(retentionHours);
        List<File> filesToPurge = fileRepository.findByStatusAndDeletedAtBefore(FileStatus.DELETED, expireTime);

        log.info("开始清理 {} 个超期文件（保留时间：{}小时）", filesToPurge.size(), retentionHours);

        for (File file : filesToPurge) {
            try {
                // 检查 MinIO 对象是否存在
                boolean exists = minioStorageService.exists(file.getBucket(), file.getObjectName());
                if (exists) {
                    // 删除 MinIO 对象
                    minioStorageService.delete(file.getBucket(), file.getObjectName());
                    log.info("MinIO 对象删除成功: fileId={}, objectName={}", file.getId(), file.getObjectName());
                } else {
                    log.warn("MinIO 对象不存在，跳过删除: fileId={}, objectName={}", file.getId(), file.getObjectName());
                }

                // 更新文件状态为 PURGED
                file.setStatus(FileStatus.PURGED);
                fileRepository.save(file);

                log.info("文件已物理删除并标记为PURGED: fileId={}, ownerId={}", file.getId(), file.getOwnerId());
            } catch (Exception e) {
                log.error("物理删除失败: fileId={}, error={}", file.getId(), e.getMessage(), e);
            }
        }

        log.info("超期文件清理完成");
    }
}
