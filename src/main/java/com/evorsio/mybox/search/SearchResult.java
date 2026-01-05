package com.evorsio.mybox.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 搜索结果项DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private UUID fileId;  // 文件记录ID

    private UUID ownerId;  // 所有者ID

    private String fileName;  // 文件名

    private String contentType;  // MIME类型

    private Long fileSize;  // 文件大小

    private LocalDateTime createdAt;  // 创建时间

    private LocalDateTime updatedAt;  // 更新时间

    private UUID folderId;  // 所属文件夹ID

    // ==================== 搜索相关字段 ====================

    private Double relevanceScore;  // 相关性得分（0-1）

    private Double similarityScore;  // 语义相似度得分（0-1，仅语义搜索）

    private String highlight;  // 高亮摘要（显示关键词上下文）

    private String matchedField;  // 匹配的字段（fileName, content, tags等）

    private SearchType matchType;  // 匹配类型（FULLTEXT, SEMANTIC, HYBRID）

    // ==================== 文件元数据 ====================

    private String description;  // 文件描述

    private String tags;  // 文件标签

    private String previewUrl;  // 预览URL（如果有）

    private String thumbnailUrl;  // 缩略图URL（如果有）

    /**
     * 搜索结果元数据（用于调试和展示）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMetadata {
        private boolean fulltextMatched;  // 是否匹配全文搜索
        private boolean semanticMatched;  // 是否匹配语义搜索
        private Double fulltextScore;  // 全文搜索得分
        private Double semanticScore;  // 语义搜索得分
        private String indexingModel;  // 使用的嵌入模型
        private Integer indexVersion;  // 索引版本
    }

    private SearchMetadata metadata;
}
