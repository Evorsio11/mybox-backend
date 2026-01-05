package com.evorsio.mybox.file;

import java.util.Optional;
import java.util.UUID;

/**
 * 文件服务接口
 * <p>
 * 管理 MinIO 中实际存储的文件，支持内容去重和引用计数
 */
public interface FileService {

    /**
     * 根据文件哈希值查找文件
     *
     * @param fileHash 文件内容的 SHA-256 哈希值
     * @return 文件（如果存在）
     */
    Optional<File> findByFileHash(String fileHash);

    /**
     * 检查是否存在相同内容的文件
     *
     * @param fileHash 文件内容的 SHA-256 哈希值
     * @return 是否存在
     */
    boolean existsByFileHash(String fileHash);

    /**
     * 创建新的文件
     *
     * @param fileHash    文件内容的 SHA-256 哈希值
     * @param objectName  MinIO 对象名称
     * @param bucket      MinIO 存储桶
     * @param contentType MIME 类型
     * @param size        文件大小
     * @return 创建的文件
     */
    File createFile(String fileHash, String objectName, String bucket, String contentType, Long size);

    /**
     * 获取或创建文件（用于内容去重）
     * <p>
     * 如果已存在相同哈希的文件，则增加引用计数并返回
     * 否则创建新的文件
     *
     * @param fileHash    文件内容的 SHA-256 哈希值
     * @param objectName  MinIO 对象名称（仅在创建新文件时使用）
     * @param bucket      MinIO 存储桶
     * @param contentType MIME 类型
     * @param size        文件大小
     * @return 文件和是否为新创建的标志
     */
    FileResult getOrCreateFile(String fileHash, String objectName, String bucket, String contentType, Long size);

    /**
     * 增加文件的引用计数
     *
     * @param fileId 文件 ID
     */
    void incrementReferenceCount(UUID fileId);

    /**
     * 减少文件的引用计数
     * <p>
     * 当引用计数降为 0 时，可以考虑删除实际存储
     *
     * @param fileId 文件 ID
     * @return 新的引用计数
     */
    int decrementReferenceCount(UUID fileId);

    /**
     * 尝试删除文件（如果无引用）
     *
     * @param fileId 文件 ID
     * @return 是否成功删除
     */
    boolean tryDeleteFile(UUID fileId);

    /**
     * 清理所有无引用的文件
     *
     * @return 清理的文件数量
     */
    int cleanupOrphanedFiles();

    /**
     * 获取实际存储使用量（去重后）
     *
     * @return 总存储大小（字节）
     */
    long getActualStorageSize();

    /**
     * 文件操作结果
     */
    record FileResult(File file, boolean isNew) {
    }
}
