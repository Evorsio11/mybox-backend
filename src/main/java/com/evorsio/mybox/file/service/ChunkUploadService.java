package com.evorsio.mybox.file.service;

import com.evorsio.mybox.file.dto.*;

import java.util.UUID;

public interface ChunkUploadService {
    /**
     * 初始化分片上传
     * @param ownerId 用户ID
     * @param request 初始化请求
     * @return 初始化响应
     */
    ChunkInitResponse initUpload(UUID ownerId, ChunkInitRequest request);

    /**
     * 上传分片
     * @param ownerId 用户ID
     * @param request 上传分片请求
     * @return 上传分片响应
     */
    UploadChunkResponse uploadChunk(UUID ownerId, UploadChunkRequest request);

    /**
     * 合并分片
     * @param ownerId 用户ID
     * @param uploadId 上传会话ID
     * @return 合并响应
     */
    ChunkMergeResponse mergeChunks(UUID ownerId, UUID uploadId);

    /**
     * 取消上传
     * @param ownerId 用户ID
     * @param uploadId 上传会话ID
     */
    void cancelUpload(UUID ownerId, UUID uploadId);

    /**
     * 查询上传进度
     * @param ownerId 用户ID
     * @param uploadId 上传会话ID
     * @return 进度响应
     */
    ChunkProgressResponse getProgress(UUID ownerId, UUID uploadId);

    /**
     * 断点续传
     * @param ownerId 用户ID
     * @param uploadId 上传会话ID
     * @return 断点续传响应
     */
    ChunkResumeResponse resumeUpload(UUID ownerId, UUID uploadId);
}
