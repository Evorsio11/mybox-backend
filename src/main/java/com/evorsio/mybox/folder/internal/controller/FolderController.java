package com.evorsio.mybox.folder.internal.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evorsio.mybox.common.ApiResponse;
import com.evorsio.mybox.folder.FolderCreateRequest;
import com.evorsio.mybox.folder.FolderMetadataUpdateRequest;
import com.evorsio.mybox.folder.FolderMoveRequest;
import com.evorsio.mybox.folder.FolderRenameRequest;
import com.evorsio.mybox.folder.FolderResponse;
import com.evorsio.mybox.folder.FolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;

    @PostMapping
    public ApiResponse<FolderResponse> createFolder(
            Authentication authentication,
            @Valid @RequestBody FolderCreateRequest request
    ){
        UUID userId = (UUID) authentication.getPrincipal();
        return ApiResponse.success(folderService.createFolder(request,userId));
    }


    @GetMapping("/{folderId}/details")
    public ApiResponse<FolderResponse> getFolderDetails(
            Authentication authentication,
            @PathVariable UUID folderId
    ){
        UUID userId = (UUID) authentication.getPrincipal();
        return ApiResponse.success(folderService.getFolderDetails(folderId,userId));
    }

    /**
     * 获取用户的根文件夹列表（懒加载第一层）
     */
    @GetMapping("/roots")
    public ApiResponse<List<FolderResponse>> getRootFolders(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ApiResponse.success(folderService.getRootFolders(userId));
    }

    /**
     * 获取指定父文件夹的直接子文件夹列表（懒加载）
     *
     * @param parentFolderId 父文件夹 ID，可选（为 null 时返回根文件夹）
     */
    @GetMapping("/children")
    public ApiResponse<List<FolderResponse>> getChildFolders(
            Authentication authentication,
            @RequestParam(required = false) UUID parentFolderId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ApiResponse.success(folderService.getChildFolders(parentFolderId, userId));
    }

    /**
     * 重命名文件夹
     */
    @PutMapping("/{folderId}/rename")
    public ApiResponse<FolderResponse> renameFolder(
            Authentication authentication,
            @PathVariable UUID folderId,
            @Valid @RequestBody FolderRenameRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ApiResponse.success(folderService.renameFolder(folderId, request.getFolderName(), userId));
    }

    /**
     * 移动文件夹到新的父文件夹
     */
    @PutMapping("/{folderId}/move")
    public ApiResponse<FolderResponse> moveFolder(
            Authentication authentication,
            @PathVariable UUID folderId,
            @Valid @RequestBody FolderMoveRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ApiResponse.success(folderService.moveFolder(folderId, request.getTargetParentFolderId(), userId));
    }

    /**
     * 更新文件夹元数据（部分更新）
     */
    @PatchMapping("/{folderId}")
    public ApiResponse<FolderResponse> updateMetadata(
            Authentication authentication,
            @PathVariable UUID folderId,
            @RequestBody FolderMetadataUpdateRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ApiResponse.success(folderService.updateMetadata(folderId, request, userId));
    }

    /**
     * 删除文件夹（软删除）
     */
    @DeleteMapping("/{folderId}")
    public ApiResponse<Void> deleteFolder(
            Authentication authentication,
            @PathVariable UUID folderId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        folderService.deleteFolder(folderId, userId);
        return ApiResponse.success();
    }

    /**
     * 恢复已删除的文件夹
     */
    @PostMapping("/{folderId}/restore")
    public ApiResponse<FolderResponse> restoreFolder(
            Authentication authentication,
            @PathVariable UUID folderId
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ApiResponse.success(folderService.restoreFolder(folderId, userId));
    }
}
