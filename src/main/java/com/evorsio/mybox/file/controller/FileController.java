package com.evorsio.mybox.file.controller;

import com.evorsio.mybox.file.domain.File;
import com.evorsio.mybox.file.dto.FileIdRequest;
import com.evorsio.mybox.file.dto.FileUploadRequest;
import com.evorsio.mybox.file.dto.FileUploadResponse;
import com.evorsio.mybox.file.service.FileService;
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

    @PostMapping("/upload")
    public ResponseEntity<List<FileUploadResponse>> upload(
            Authentication authentication,
            @Valid @ModelAttribute FileUploadRequest request
    ) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());

        List<FileUploadResponse> results = request.getFiles().stream()
                .map(file -> {
                    try {
                        File uploadedFile = fileService.uploadFile(
                                ownerId,
                                file.getOriginalFilename(),
                                file.getSize(),
                                file.getContentType(),
                                file.getInputStream()
                        );
                        return new FileUploadResponse(uploadedFile.getOriginalFileName(), true, "上传成功");
                    } catch (Exception e) {
                        log.error("上传文件失败: {}", file.getOriginalFilename(), e);
                        return new FileUploadResponse(file.getOriginalFilename(), false, e.getMessage());
                    }
                })
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }


    @PostMapping("/download")
    public ResponseEntity<InputStreamResource> download(
            Authentication authentication,
            @Valid @RequestBody FileIdRequest request
    ) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        File file = fileService.getActiveFileById(ownerId, request.getFileId());
        InputStream inputStream = fileService.downloadFile(ownerId, request.getFileId());
        InputStreamResource resource = new InputStreamResource(inputStream);

        // 对文件名做 URL 编码
        String safeFileName = URLEncoder.encode(file.getOriginalFileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + safeFileName)
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(resource);
    }


    // 删除文件
    @PostMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @Valid @RequestBody FileIdRequest request
    ) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());

        fileService.deleteFile(ownerId, request.getFileId());
    }

    // 列出所有文件
    @GetMapping
    public List<File> listFiles(Authentication authentication) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        return fileService.listFiles(ownerId);
    }

    // 列出已删除文件
    @GetMapping("/deleted")
    public List<File> listDeletedFiles(Authentication authentication) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());
        return fileService.listDeletedFiles(ownerId);
    }

    // 恢复已删除文件
    @PostMapping("/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(
            Authentication authentication,
            @RequestBody FileIdRequest request
    ) {
        UUID ownerId = UUID.fromString(authentication.getDetails().toString());

        fileService.restoreFile(ownerId, request.getFileId());
    }
}
