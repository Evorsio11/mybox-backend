package com.evorsio.mybox.file.internal.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.evorsio.mybox.file.File;

/**
 * 文件数据访问层
 * <p>
 * 管理 MinIO 中实际存储的文件，支持内容去重
 */
public interface FileRepository extends JpaRepository<File, UUID> {

    /**
     * 根据文件哈希值查找文件（用于内容去重）
     *
     * @param fileHash 文件内容的 SHA-256 哈希值
     * @return 文件（如果存在）
     */
    Optional<File> findByFileHash(String fileHash);

    /**
     * 检查是否存在指定哈希值的文件
     *
     * @param fileHash 文件内容的 SHA-256 哈希值
     * @return 是否存在
     */
    boolean existsByFileHash(String fileHash);

    /**
     * 增加引用计数
     *
     * @param id 文件 ID
     * @return 更新的行数
     */
    @Modifying
    @Query("UPDATE File f SET f.referenceCount = f.referenceCount + 1, f.updatedAt = CURRENT_TIMESTAMP WHERE f.id = :id")
    int incrementReferenceCount(@Param("id") UUID id);

    /**
     * 减少引用计数
     *
     * @param id 文件 ID
     * @return 更新的行数
     */
    @Modifying
    @Query("UPDATE File f SET f.referenceCount = f.referenceCount - 1, f.updatedAt = CURRENT_TIMESTAMP WHERE f.id = :id AND f.referenceCount > 0")
    int decrementReferenceCount(@Param("id") UUID id);

    /**
     * 获取当前引用计数
     *
     * @param id 文件 ID
     * @return 引用计数
     */
    @Query("SELECT f.referenceCount FROM File f WHERE f.id = :id")
    Integer getReferenceCount(@Param("id") UUID id);

    /**
     * 查找所有无引用的文件（用于清理任务）
     *
     * @return 无引用的文件列表
     */
    @Query("SELECT f FROM File f WHERE f.referenceCount <= 0")
    java.util.List<File> findOrphanedFiles();

    /**
     * 删除无引用的文件记录
     *
     * @return 删除的行数
     */
    @Modifying
    @Query("DELETE FROM File f WHERE f.referenceCount <= 0")
    int deleteOrphanedFiles();

    /**
     * 统计实际存储使用量（去重后的总大小）
     *
     * @return 总存储大小（字节）
     */
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM File f WHERE f.referenceCount > 0")
    long sumActualStorageSize();

    /**
     * 统计文件数量
     *
     * @return 文件数量
     */
    @Query("SELECT COUNT(f) FROM File f WHERE f.referenceCount > 0")
    long countActiveFiles();
}
