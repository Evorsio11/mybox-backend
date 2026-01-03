package com.evorsio.mybox.folder;

import java.util.List;
import java.util.UUID;

public interface FolderService {
    FolderResponse createFolder(FolderCreateRequest request, UUID userId);

    FolderResponse getFolderDetails(UUID request, UUID userId);

    /**
     * 获取用户的根文件夹列表（仅第一层，懒加载）
     *
     * @param userId 用户 ID
     * @return 根文件夹列表（parentFolderId 为 null）
     */
    List<FolderResponse> getRootFolders(UUID userId);

    /**
     * 获取指定父文件夹的直接子文件夹列表（懒加载）
     *
     * @param parentFolderId 父文件夹 ID（null 表示查询根目录）
     * @param userId         用户 ID
     * @return 直接子文件夹列表
     */
    List<FolderResponse> getChildFolders(UUID parentFolderId, UUID userId);

    /**
     * 重命名文件夹
     *
     * @param folderId  文件夹 ID
     * @param newName   新文件夹名称
     * @param userId    用户 ID
     * @return 更新后的文件夹信息
     */
    FolderResponse renameFolder(UUID folderId, String newName, UUID userId);

    /**
     * 移动文件夹到新的父文件夹
     *
     * @param folderId         要移动的文件夹 ID
     * @param targetParentId   目标父文件夹 ID（null 表示移动到根目录）
     * @param userId           用户 ID
     * @return 移动后的文件夹信息
     */
    FolderResponse moveFolder(UUID folderId, UUID targetParentId, UUID userId);

    /**
     * 更新文件夹元数据（部分更新）
     *
     * @param folderId 文件夹 ID
     * @param request  元数据更新请求
     * @param userId   用户 ID
     * @return 更新后的文件夹信息
     */
    FolderResponse updateMetadata(UUID folderId, FolderMetadataUpdateRequest request, UUID userId);

    /**
     * 删除文件夹（软删除）
     *
     * @param folderId 文件夹 ID
     * @param userId   用户 ID
     */
    void deleteFolder(UUID folderId, UUID userId);

    /**
     * 恢复已删除的文件夹
     *
     * @param folderId 文件夹 ID
     * @param userId   用户 ID
     * @return 恢复后的文件夹信息
     */
    FolderResponse restoreFolder(UUID folderId, UUID userId);
}
