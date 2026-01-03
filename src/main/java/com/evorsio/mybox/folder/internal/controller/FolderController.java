package com.evorsio.mybox.folder.internal.controller;

import com.evorsio.mybox.folder.FolderCreateRequest;
import com.evorsio.mybox.folder.FolderMetadataUpdateRequest;
import com.evorsio.mybox.folder.FolderMoveRequest;
import com.evorsio.mybox.folder.FolderRenameRequest;
import com.evorsio.mybox.folder.FolderResponse;
import com.evorsio.mybox.folder.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;

    @PostMapping
    public FolderResponse createFolder(
            Authentication authentication,
            @Valid @RequestBody FolderCreateRequest request
    ){
        UUID userId = (UUID) authentication.getPrincipal();
        return folderService.createFolder(request,userId);
    }


    @GetMapping("/{folderId}/details")
    public FolderResponse getFolderDetails(
            Authentication authentication,
            @PathVariable UUID folderId
    ){
        UUID userId = (UUID) authentication.getPrincipal();
        return folderService.getFolderDetails(folderId,userId);
    }

    /**
     * 获取用户的根文件夹列表（懒加载第一层）
     */
    @GetMapping("/roots")
    public List<FolderResponse> getRootFolders(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return folderService.getRootFolders(userId);
    }

    /**
     * 获取指定父文件夹的直接子文件夹列表（懒加载）
     *
     * @param parentFolderId 父文件夹 ID，可选（为 null 时返回根文件夹）
     */
    @GetMapping("/children")
    public List<FolderResponse> getChildFolders(
            Authentication authentication,
            @RequestParam(required = false) UUID parentFolderId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return folderService.getChildFolders(parentFolderId, userId);
    }

    /**
     * 重命名文件夹
     */
    @PutMapping("/{folderId}/rename")
    public FolderResponse renameFolder(
            Authentication authentication,
            @PathVariable UUID folderId,
            @Valid @RequestBody FolderRenameRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return folderService.renameFolder(folderId, request.getFolderName(), userId);
    }

    /**
     * 移动文件夹到新的父文件夹
     */
    @PutMapping("/{folderId}/move")
    public FolderResponse moveFolder(
            Authentication authentication,
            @PathVariable UUID folderId,
            @Valid @RequestBody FolderMoveRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return folderService.moveFolder(folderId, request.getTargetParentFolderId(), userId);
    }

    /**
     * 更新文件夹元数据（部分更新）
     */
    @PatchMapping("/{folderId}")
    public FolderResponse updateMetadata(
            Authentication authentication,
            @PathVariable UUID folderId,
            @RequestBody FolderMetadataUpdateRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return folderService.updateMetadata(folderId, request, userId);
    }

    /**
     * 删除文件夹（软删除）
     */
    @DeleteMapping("/{folderId}")
    public void deleteFolder(
            Authentication authentication,
            @PathVariable UUID folderId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        folderService.deleteFolder(folderId, userId);
    }

    /**
     * 恢复已删除的文件夹
     */
    @PostMapping("/{folderId}/restore")
    public FolderResponse restoreFolder(
            Authentication authentication,
            @PathVariable UUID folderId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return folderService.restoreFolder(folderId, userId);
    }
}
