package com.evorsio.mybox.file;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一分片上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChunkUploadResponse {
    /**
     * 上传会话ID
     */
    private UUID uploadId;

    /**
     * 当前分片编号
     */
    private Integer chunkNumber;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;

    /**
     * 上传进度（0-100）
     */
    private Double progress;

    /**
     * 是否已完成（所有分片上传完成并合并）
     */
    private Boolean completed;

    /**
     * 文件ID（仅在completed=true时返回）
     */
    private UUID fileId;

    /**
     * 文件Hash（仅在completed=true时返回）
     */
    private String fileHash;

    /**
     * 状态信息
     */
    private String message;
}
