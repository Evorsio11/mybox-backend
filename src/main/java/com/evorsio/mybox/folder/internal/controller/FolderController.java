package com.evorsio.mybox.folder.internal.controller;

import java.util.List;
import java.util.UUID;

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

import com.evorsio.mybox.auth.CurrentUser;
import com.evorsio.mybox.auth.UserPrincipal;
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
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody FolderCreateRequest request
    ){
        return ApiResponse.success(folderService.createFolder(request, user.getId()));
    }


    @GetMapping("/{folderId}/details")
    public ApiResponse<FolderResponse> getFolderDetails(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID folderId
    ){
        return ApiResponse.success(folderService.getFolderDetails(folderId, user.getId()));
    }

    /**
     * 获取用户的根文件夹列表（懒加载第一层）
     */
    @GetMapping("/roots")
    public ApiResponse<List<FolderResponse>> getRootFolders(@CurrentUser UserPrincipal user) {
        return ApiResponse.success(folderService.getRootFolders(user.getId()));
    }

    /**
     * 获取指定父文件夹的直接子文件夹列表（懒加载）
     *
     * @param parentFolderId 父文件夹 ID，可选（为 null 时返回根文件夹）
     */
    @GetMapping("/children")
    public ApiResponse<List<FolderResponse>> getChildFolders(
            @CurrentUser UserPrincipal user,
            @RequestParam(required = false) UUID parentFolderId
    ) {
        return ApiResponse.success(folderService.getChildFolders(parentFolderId, user.getId()));
    }

    /**
     * 重命名文件夹
     */
    @PutMapping("/{folderId}/rename")
    public ApiResponse<FolderResponse> renameFolder(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID folderId,
            @Valid @RequestBody FolderRenameRequest request
    ) {
        return ApiResponse.success(folderService.renameFolder(folderId, request.getFolderName(), user.getId()));
    }

    /**
     * 移动文件夹到新的父文件夹
     */
    @PutMapping("/{folderId}/move")
    public ApiResponse<FolderResponse> moveFolder(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID folderId,
            @Valid @RequestBody FolderMoveRequest request
    ) {
        return ApiResponse.success(folderService.moveFolder(folderId, request.getTargetParentFolderId(), user.getId()));
    }

    /**
     * 更新文件夹元数据（部分更新）
     */
    @PatchMapping("/{folderId}")
    public ApiResponse<FolderResponse> updateMetadata(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID folderId,
            @RequestBody FolderMetadataUpdateRequest request
    ) {
        return ApiResponse.success(folderService.updateMetadata(folderId, request, user.getId()));
    }

    /**
     * 删除文件夹（软删除）
     */
    @DeleteMapping("/{folderId}")
    public ApiResponse<Void> deleteFolder(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID folderId
    ) {
        folderService.deleteFolder(folderId, user.getId());
        return ApiResponse.success();
    }

    /**
     * 恢复已删除的文件夹
     */
    @PostMapping("/{folderId}/restore")
    public ApiResponse<FolderResponse> restoreFolder(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID folderId
    ) {
        return ApiResponse.success(folderService.restoreFolder(folderId, user.getId()));
    }
}
