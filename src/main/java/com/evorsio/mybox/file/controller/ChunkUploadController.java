package com.evorsio.mybox.file.controller;

import com.evorsio.mybox.common.response.ApiResponse;
import com.evorsio.mybox.file.dto.*;
import com.evorsio.mybox.file.service.ChunkUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/files/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {
    private final ChunkUploadService chunkUploadService;

    /**
     * 初始化分片上传
     * POST /api/files/chunk/init
     */
    @PostMapping("/init")
    public ApiResponse<ChunkInitResponse> initUpload(
            Authentication authentication,
            @Valid @RequestBody ChunkInitRequest request) {

        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        ChunkInitResponse response = chunkUploadService.initUpload(ownerId, request);

        return ApiResponse.success("初始化上传成功", response);
    }

    /**
     * 上传分片
     * POST /api/files/chunk/upload
     */
    @PostMapping("/upload")
    public ApiResponse<UploadChunkResponse> uploadChunk(
            Authentication authentication,
            @Valid @ModelAttribute UploadChunkRequest request) {

        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        UploadChunkResponse response = chunkUploadService.uploadChunk(ownerId, request);

        return ApiResponse.success("上传分片成功", response);
    }

    /**
     * 合并分片
     * POST /api/files/chunk/merge
     */
    @PostMapping("/merge")
    public ApiResponse<ChunkMergeResponse> mergeChunks(
            Authentication authentication,
            @Valid @RequestBody ChunkMergeRequest request) {

        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        ChunkMergeResponse response = chunkUploadService.mergeChunks(ownerId, request.getUploadId());

        return ApiResponse.success("合并分片成功", response);
    }

    /**
     * 取消上传
     * DELETE /api/files/chunk/cancel
     */
    @DeleteMapping("/cancel")
    public ApiResponse<Void> cancelUpload(
            Authentication authentication,
            @Valid @RequestBody ChunkMergeRequest request) {

        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        chunkUploadService.cancelUpload(ownerId, request.getUploadId());

        return ApiResponse.success("取消上传成功");
    }

    /**
     * 查询上传进度
     * GET /api/files/chunk/progress/{uploadId}
     */
    @GetMapping("/progress/{uploadId}")
    public ApiResponse<ChunkProgressResponse> getProgress(
            Authentication authentication,
            @PathVariable UUID uploadId) {

        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        ChunkProgressResponse response = chunkUploadService.getProgress(ownerId, uploadId);

        return ApiResponse.success("查询进度成功", response);
    }

    /**
     * 断点续传
     * GET /api/files/chunk/resume/{uploadId}
     */
    @GetMapping("/resume/{uploadId}")
    public ApiResponse<ChunkResumeResponse> resumeUpload(
            Authentication authentication,
            @PathVariable UUID uploadId) {

        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        ChunkResumeResponse response = chunkUploadService.resumeUpload(ownerId, uploadId);

        return ApiResponse.success("断点续传成功", response);
    }
}
