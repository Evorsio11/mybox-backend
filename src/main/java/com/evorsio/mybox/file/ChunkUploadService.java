package com.evorsio.mybox.file;

import java.util.UUID;

public interface ChunkUploadService {
    /**
     * 统一分片上传接口（支持 Uppy XHRUpload）
     * 自动处理初始化、上传和合并
     * @param ownerId 用户ID
     * @param request 统一上传请求
     * @return 统一上传响应
     */
    UnifiedChunkUploadResponse uploadChunkUnified(UUID ownerId, UnifiedChunkUploadRequest request);
}
