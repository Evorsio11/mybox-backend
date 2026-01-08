package com.evorsio.mybox.search.internal.repository;

import com.evorsio.mybox.search.FileIndex;
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
public interface FileIndexRepository extends JpaRepository<FileIndex, UUID> {

    /**
     * 根据文件ID查找文档索引
     */
    Optional<FileIndex> findByFileId(UUID fileId);

    /**
     * 根据所有者ID查找所有文档索引
     */
    List<FileIndex> findByOwnerId(UUID ownerId);

    /**
     * 根据索引状态查找文档索引
     */
    List<FileIndex> findByStatus(IndexStatus status);

    /**
     * 查找需要重新索引的文档
     */
    @Query("SELECT di FROM FileIndex di WHERE di.fileId = :fileId " +
           "AND (di.status = 'PENDING' OR di.status = 'FAILED' OR di.status = 'OUTDATED' " +
           "OR di.fileModifiedAt < :fileModifiedAt)")
    Optional<FileIndex> findNeedsReindex(@Param("fileId") UUID fileId,
                                         @Param("fileModifiedAt") LocalDateTime fileModifiedAt);

    /**
     * 全文搜索（使用PostgreSQL tsvector）
     */
    @Query(value = "SELECT * FROM document_indices " +
           "WHERE owner_id = :ownerId " +
           "AND text_search_vector @@ to_tsquery('simple', :query) " +
           "AND status = 'INDEXED' " +
           "ORDER BY ts_rank(text_search_vector, to_tsquery('simple', :query)) DESC " +
           "LIMIT :limit",
           nativeQuery = true)
    List<FileIndex> fullTextSearch(@Param("ownerId") UUID ownerId,
                                   @Param("query") String query,
                                   @Param("limit") int limit);

    /**
     * 统计用户的已索引文档数量
     */
    long countByOwnerIdAndStatus(UUID ownerId, IndexStatus status);

    /**
     * 删除指定文件的所有索引
     */
    void deleteByFileId(UUID fileId);
}
