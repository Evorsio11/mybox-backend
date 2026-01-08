package com.evorsio.mybox.search.internal.repository;

import com.evorsio.mybox.search.FileVector;
import com.evorsio.mybox.search.IndexStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileVectorRepository extends JpaRepository<FileVector, UUID> {

    /**
     * 根据文件ID查找向量（唯一）
     */
    Optional<FileVector> findByFileId(UUID fileId);

    /**
     * 根据所有者ID查找所有向量
     */
    List<FileVector> findByOwnerId(UUID ownerId);

    /**
     * 根据索引状态查找向量
     */
    List<FileVector> findByStatus(IndexStatus status);

    /**
     * 查找需要重新生成向量的文件
     */
    @Query("SELECT fv FROM FileVector fv WHERE fv.fileId = :fileId " +
           "AND (fv.status = 'PENDING' OR fv.status = 'FAILED' OR fv.status = 'OUTDATED' " +
           "OR fv.fileModifiedAt < :fileModifiedAt)")
    Optional<FileVector> findNeedsRegeneration(@Param("fileId") UUID fileId,
                                                @Param("fileModifiedAt") LocalDateTime fileModifiedAt);

    /**
     * 向量相似度搜索（使用pgvector）
     * 注意：需要pgvector扩展，使用 <=> 操作符计算余弦距离
     */
    @Query(value = "SELECT * FROM file_vectors " +
           "WHERE owner_id = :ownerId " +
           "AND status = 'INDEXED' " +
           "AND embedding <=> CAST(:queryVector AS vector(1024)) < :maxDistance " +
           "ORDER BY embedding <=> CAST(:queryVector AS vector(1024)) " +
           "LIMIT :limit",
           nativeQuery = true)
    List<FileVector> semanticSearch(@Param("ownerId") UUID ownerId,
                                     @Param("queryVector") String queryVector,
                                     @Param("maxDistance") double maxDistance,
                                     @Param("limit") int limit);

    /**
     * 统计用户的已向量化文件数量
     */
    long countByOwnerIdAndStatus(UUID ownerId, IndexStatus status);

    /**
     * 删除指定文件的向量
     */
    void deleteByFileId(UUID fileId);

    /**
     * 检查向量是否已存在
     */
    boolean existsByFileId(UUID fileId);
}
