package com.evorsio.mybox.file.internal.controller;

import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.file.File;
import com.evorsio.mybox.file.FileIdRequest;
import com.evorsio.mybox.file.FileUploadResponse;
import com.evorsio.mybox.file.FileConfigService;
import com.evorsio.mybox.file.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    private final FileConfigService fileConfigService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<FileUploadResponse>> upload(
            Authentication authentication,
            @RequestParam("files") List<MultipartFile> files
    ) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());

        // 验证文件列表不为空
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("文件列表不能为空");
        }

        List<FileUploadResponse> results = files.stream()
                .map(file -> {
                    try {
                        File uploadedFile = fileService.uploadFile(
                                ownerId,
                                file.getOriginalFilename(),
                                file.getSize(),
                                file.getContentType(),
                                file.getInputStream()
                        );

                        // 上传成功后，如果文件较大且分片上传已启用，给出建议
                        long chunkUploadMinSize = fileConfigService.getChunkUploadMinSize();
                        if (file.getSize() >= chunkUploadMinSize && fileConfigService.isChunkUploadEnabled()) {
                            log.info("大文件已通过普通上传成功，建议下次使用分片上传: fileName={}, size={}",
                                    file.getOriginalFilename(), file.getSize());
                            return new FileUploadResponse(
                                    uploadedFile.getOriginalFileName(),
                                    true,
                                    "上传成功（提示：大文件建议使用分片上传接口以获得更好的上传体验）"
                            );
                        }

                        return new FileUploadResponse(uploadedFile.getOriginalFileName(), true, "上传成功");
                    } catch (Exception e) {
                        log.error("上传文件失败: {}", file.getOriginalFilename(), e);
                        return new FileUploadResponse(file.getOriginalFilename(), false, e.getMessage());
                    }
                })
                .toList();

        return ApiResponse.success("文件上传完成", results);
    }

    /**
     * 文件下载接口（保持 ResponseEntity 格式以支持流式传输）
     */
    @PostMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            Authentication authentication,
            @Valid @RequestBody FileIdRequest request,
            HttpServletRequest httpServletRequest) {

        // 获取当前用户的 ID
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());

        // 获取文件对象
        File file = fileService.getActiveFileById(ownerId, request.getFileId());

        // 获取文件输入流
        InputStream inputStream = fileService.downloadFile(ownerId, request.getFileId());
        InputStreamResource resource = new InputStreamResource(inputStream);

        // 对文件名进行 URL 编码
        String safeFileName = URLEncoder.encode(file.getOriginalFileName(), StandardCharsets.UTF_8);

        // 检查 Range 请求头
        String rangeHeader = httpServletRequest.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null) {
            try {
                // 处理 Range 请求，返回文件的部分内容
                String[] ranges = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(ranges[0]);
                long end = (ranges.length > 1) ? Long.parseLong(ranges[1]) : file.getSize() - 1;

                // 检查请求的范围是否合法
                if (start >= file.getSize()) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).body(null);
                }

                // 限制 end 不能超过文件大小
                end = Math.min(end, file.getSize() - 1);

                // 获取部分文件输入流
                InputStream partialInputStream = fileService.downloadPartialFile(ownerId, request.getFileId(), start, end);
                InputStreamResource partialResource = new InputStreamResource(partialInputStream);

                // 设置响应头，指示返回的文件部分
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + file.getSize());
                headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(end - start + 1));
                headers.add(HttpHeaders.CONTENT_TYPE, file.getContentType());

                // 返回 206 Partial Content
                return new ResponseEntity<>(partialResource, headers, HttpStatus.PARTIAL_CONTENT);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }

        // 如果没有 Range 请求头，返回整个文件
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + safeFileName)
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(resource);
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(
            Authentication authentication,
            @Valid @RequestBody FileIdRequest request
    ) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        fileService.deleteFile(ownerId, request.getFileId());
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<File>> listFiles(Authentication authentication) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        List<File> files = fileService.listFiles(ownerId);
        return ApiResponse.success("获取文件列表成功", files);
    }

    @GetMapping("/deleted")
    public ApiResponse<List<File>> listDeletedFiles(Authentication authentication) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        List<File> files = fileService.listDeletedFiles(ownerId);
        return ApiResponse.success("获取已删除文件列表成功", files);
    }

    @PostMapping("/restore")
    public ApiResponse<Void> restore(
            Authentication authentication,
            @RequestBody FileIdRequest request
    ) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        fileService.restoreFile(ownerId, request.getFileId());
        return ApiResponse.success();
    }
}
