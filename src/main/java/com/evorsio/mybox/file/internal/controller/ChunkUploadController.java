package com.evorsio.mybox.file.internal.controller;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evorsio.mybox.auth.CurrentUser;
import com.evorsio.mybox.auth.UserPrincipal;
import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.file.ChunkUploadService;
import com.evorsio.mybox.file.UnifiedChunkUploadRequest;
import com.evorsio.mybox.file.UnifiedChunkUploadResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/files/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {
    private final ChunkUploadService chunkUploadService;

    /**
     * 统一分片上传接口（支持 Uppy XHRUpload）
     * POST /api/files/chunk/unified
     * 
     * 单接口设计，自动处理初始化、上传和合并
     * 适配 Uppy 的 XHRUpload 插件
     */
    @PostMapping("/unified")
    public ApiResponse<UnifiedChunkUploadResponse> uploadChunkUnified(
            @CurrentUser UserPrincipal user,
            @Valid @ModelAttribute UnifiedChunkUploadRequest request) {

        long startTime = System.currentTimeMillis();
        UUID ownerId = user.getId();

        log.info("[分片上传-开始] ownerId={}, fileIdentifier={}, fileName={}, chunkNumber={}/{}, fileSize={}",
                ownerId, request.getFileIdentifier(), request.getOriginalFileName(),
                request.getChunkNumber(), request.getTotalChunks(), request.getFileSize());

        try {
            UnifiedChunkUploadResponse response = chunkUploadService.uploadChunkUnified(ownerId, request);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[分片上传-完成] ownerId={}, fileIdentifier={}, chunkNumber={}, 耗时={}ms, completed={}",
                    ownerId, request.getFileIdentifier(), request.getChunkNumber(), duration, response.getCompleted());

            return ApiResponse.success(response.getCompleted() ? "上传完成" : "分片上传成功", response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[分片上传-失败] ownerId={}, fileIdentifier={}, chunkNumber={}, 耗时={}ms, error={}",
                    ownerId, request.getFileIdentifier(), request.getChunkNumber(), duration, e.getMessage(), e);
            throw e;
        }
    }
}
