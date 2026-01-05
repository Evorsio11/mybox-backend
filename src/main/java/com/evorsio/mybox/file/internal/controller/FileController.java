package com.evorsio.mybox.file.internal.controller;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.evorsio.mybox.auth.CurrentUser;
import com.evorsio.mybox.auth.UserPrincipal;
import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.file.FileConfigService;
import com.evorsio.mybox.file.FileIdRequest;
import com.evorsio.mybox.file.FileMoveRequest;
import com.evorsio.mybox.file.FileRecord;
import com.evorsio.mybox.file.FileRecordService;
import com.evorsio.mybox.file.FileUploadResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileRecordService fileRecordService;
    private final FileConfigService fileConfigService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<FileUploadResponse>> upload(
            @CurrentUser UserPrincipal user,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folderId", required = false) UUID folderId
    ) {
        UUID ownerId = user.getId();

        // 验证文件列表不为空
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("文件列表不能为空");
        }

        List<FileUploadResponse> results = files.stream()
                .map(file -> {
                    try {
                        FileRecord uploadedFileRecord = fileRecordService.uploadFileRecord(
                                ownerId,
                                folderId,
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
                                    uploadedFileRecord.getOriginalFileName(),
                                    true,
                                    "上传成功（提示：大文件建议使用分片上传接口以获得更好的上传体验）"
                            );
                        }

                        return new FileUploadResponse(uploadedFileRecord.getOriginalFileName(), true, "上传成功");
                    } catch (Exception e) {
                        log.error("上传文件失败: {}", file.getOriginalFilename(), e);
                        return new FileUploadResponse(file.getOriginalFilename(), false, e.getMessage());
                    }
                })
                .toList();

        // 统计成功和失败的数量
        long successCount = results.stream().filter(FileUploadResponse::isSuccess).count();
        long failCount = results.size() - successCount;

        // 根据结果构建更清晰的响应消息
        String message;
        if (failCount == 0) {
            message = String.format("全部上传成功（%d个文件）", successCount);
        } else if (successCount == 0) {
            message = String.format("全部上传失败（%d个文件）", failCount);
        } else {
            message = String.format("部分上传成功（成功%d个，失败%d个）", successCount, failCount);
        }

        return ApiResponse.success(message, results);
    }

    /**
     * 文件下载接口（保持 ResponseEntity 格式以支持流式传输）
     */
    @PostMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody FileIdRequest request,
            HttpServletRequest httpServletRequest) {

        // 获取当前用户的 ID
        UUID ownerId = user.getId();

        // 获取文件记录对象
        FileRecord fileRecord = fileRecordService.getActiveFileRecordById(ownerId, request.getFileId());

        // 获取文件输入流
        InputStream inputStream = fileRecordService.downloadFileRecord(ownerId, request.getFileId());
        InputStreamResource resource = new InputStreamResource(inputStream);

        // 对文件名进行 URL 编码
        String safeFileName = URLEncoder.encode(fileRecord.getOriginalFileName(), StandardCharsets.UTF_8);

        // 检查 Range 请求头
        String rangeHeader = httpServletRequest.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null) {
            try {
                // 处理 Range 请求，返回文件的部分内容
                String[] ranges = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(ranges[0]);
                long fileSize = fileRecord.getSize();
                long end = (ranges.length > 1) ? Long.parseLong(ranges[1]) : fileSize - 1;

                // 检查请求的范围是否合法
                if (start >= fileSize) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).body(null);
                }

                // 限制 end 不能超过文件大小
                end = Math.min(end, fileSize - 1);

                // 获取部分文件输入流
                InputStream partialInputStream = fileRecordService.downloadPartialFileRecord(ownerId, request.getFileId(), start, end);
                InputStreamResource partialResource = new InputStreamResource(partialInputStream);

                // 设置响应头，指示返回的文件部分
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
                headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(end - start + 1));
                headers.add(HttpHeaders.CONTENT_TYPE, fileRecord.getContentType());

                // 返回 206 Partial Content
                return new ResponseEntity<>(partialResource, headers, HttpStatus.PARTIAL_CONTENT);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }

        // 如果没有 Range 请求头，返回整个文件
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + safeFileName)
                .contentType(MediaType.parseMediaType(fileRecord.getContentType()))
                .body(resource);
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody FileIdRequest request
    ) {
        UUID ownerId = user.getId();
        fileRecordService.deleteFileRecord(ownerId, request.getFileId());
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<FileRecord>> listFiles(@CurrentUser UserPrincipal user) {
        UUID ownerId = user.getId();
        List<FileRecord> fileRecords = fileRecordService.listFileRecords(ownerId);
        return ApiResponse.success("获取文件列表成功", fileRecords);
    }

    @GetMapping("/deleted")
    public ApiResponse<List<FileRecord>> listDeletedFiles(@CurrentUser UserPrincipal user) {
        UUID ownerId = user.getId();
        List<FileRecord> fileRecords = fileRecordService.listDeletedFileRecords(ownerId);
        return ApiResponse.success("获取已删除文件列表成功", fileRecords);
    }

    @PostMapping("/restore")
    public ApiResponse<Void> restore(
            @CurrentUser UserPrincipal user,
            @RequestBody FileIdRequest request
    ) {
        UUID ownerId = user.getId();
        fileRecordService.restoreFileRecord(ownerId, request.getFileId());
        return ApiResponse.success();
    }

    /**
     * 获取指定文件夹内的文件列表
     */
    @GetMapping("/folder/{folderId}")
    public ApiResponse<List<FileRecord>> listFilesByFolder(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID folderId
    ) {
        UUID ownerId = user.getId();
        List<FileRecord> fileRecords = fileRecordService.listFileRecordsByFolder(ownerId, folderId);
        return ApiResponse.success("获取文件夹内文件列表成功", fileRecords);
    }

    /**
     * 获取未分类文件列表（folderId 为 null）
     */
    @GetMapping("/unclassified")
    public ApiResponse<List<FileRecord>> listUnclassifiedFiles(@CurrentUser UserPrincipal user) {
        UUID ownerId = user.getId();
        List<FileRecord> fileRecords = fileRecordService.listUnclassifiedFileRecords(ownerId);
        return ApiResponse.success("获取未分类文件列表成功", fileRecords);
    }

    /**
     * 移动文件到指定文件夹
     */
    @PutMapping("/{fileId}/move")
    public ApiResponse<FileRecord> moveFileToFolder(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fileId,
            @Valid @RequestBody FileMoveRequest request
    ) {
        UUID ownerId = user.getId();
        FileRecord movedFileRecord = fileRecordService.moveFileRecordToFolder(ownerId, fileId, request.getTargetFolderId());
        return ApiResponse.success("文件移动成功", movedFileRecord);
    }
}
