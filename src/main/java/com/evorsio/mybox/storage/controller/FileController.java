package com.evorsio.mybox.storage.controller;

import com.evorsio.mybox.storage.domain.File;
import com.evorsio.mybox.storage.dto.FileIdRequest;
import com.evorsio.mybox.storage.dto.FileUploadRequest;
import com.evorsio.mybox.storage.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/upload")
    public File upload(
            Authentication authentication,
            @ModelAttribute FileUploadRequest request
    ) {

        try {

            return fileService.uploadFile(
                    UUID.fromString(authentication.getDetails().toString()),
                    request.getFile().getOriginalFilename(),
                    request.getFile().getSize(),
                    request.getFile().getContentType(),
                    request.getFile().getInputStream()
            );
        } catch (Exception e) {
            throw new RuntimeException("上传文件失败", e);
        }
    }

    @PostMapping("/download")
    public ResponseEntity<InputStream> download(
            Authentication authentication,
            @RequestBody FileIdRequest request
    ) {
        UUID ownerId = UUID.fromString(authentication.getPrincipal().toString());

        File file = fileService.getActiveFileById(ownerId, request.getFileId());

        InputStream inputStream = fileService.downloadFile(ownerId, request.getFileId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(inputStream);
    }

    // 删除文件
    @PostMapping("/delete")
    public void delete(
            Authentication authentication,
            @RequestBody FileIdRequest request
    ) {
        UUID ownerId = UUID.fromString(authentication.getPrincipal().toString());

        fileService.deleteFile(ownerId, request.getFileId());
    }

    // 列出所有文件
    @GetMapping
    public List<File> listFiles(@RequestParam UUID ownerId) {
        return fileService.listFiles(ownerId);
    }

    // 列出已删除文件
    @GetMapping("/deleted")
    public List<File> listDeletedFiles(@RequestParam UUID ownerId) {
        return fileService.listDeletedFiles(ownerId);
    }

    // 恢复已删除文件
    @PostMapping("/restore")
    public void restore(
            Authentication authentication,
            @RequestBody FileIdRequest request
    ) {
        UUID ownerId = UUID.fromString(authentication.getPrincipal().toString());

        fileService.restoreFile(ownerId, request.getFileId());
    }
}
