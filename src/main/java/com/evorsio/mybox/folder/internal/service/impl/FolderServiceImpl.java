package com.evorsio.mybox.folder.internal.service.impl;

import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.folder.*;
import com.evorsio.mybox.folder.internal.exception.FolderException;
import com.evorsio.mybox.folder.internal.mapper.FolderMapper;
import com.evorsio.mybox.folder.internal.repository.FolderRepository;
import com.evorsio.mybox.folder.internal.service.FolderValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    private final FolderRepository folderRepository;
    private final FolderMapper folderMapper;
    private final FolderValidationService validationService;

    @Override
    @Transactional
    public FolderResponse createFolder(FolderCreateRequest request, UUID userId) {
        // 1. 获取主设备
        UUID primaryDeviceId = validationService.getPrimaryDeviceOrThrow(userId);

        // 2. 验证父文件夹
        Folder parentFolder = validationService.getAndValidateParentFolder(
                request.getParentFolderId(),
                userId
        );

        // 3. 检查同名文件夹
        validationService.checkDuplicateFolderName(request.getParentFolderId(), request.getFolderName());

        // 4. 计算路径和层级
        String fullPath = validationService.calculateFullPath(parentFolder, request.getFolderName());
        Integer level = validationService.calculateLevel(parentFolder);

        // 5. 构建并保存文件夹
        Folder folder = Folder.builder()
                .folderId(UUID.randomUUID())
                .userId(userId)
                .primaryDeviceId(primaryDeviceId)
                .folderName(request.getFolderName())
                .parentFolderId(request.getParentFolderId())
                .fullPath(fullPath)
                .level(level)
                .color(request.getColor())
                .iconName(request.getIconName())
                .description(request.getDescription())
                .build();

        folderRepository.save(folder);

        log.info("文件夹创建成功: folderId={}, userId={}, fullPath={}",
                folder.getFolderId(), userId, fullPath);

        return folderMapper.toResponse(folder);
    }

    @Override
    public FolderResponse getFolderDetails(UUID folderId, UUID userId) {
        Folder folder = validationService.getActiveFolderOrThrow(folderId, userId);
        return folderMapper.toResponse(folder);
    }

    @Override
    public List<FolderResponse> getRootFolders(UUID userId) {
        // 获取用户主设备
        UUID primaryDeviceId = validationService.getPrimaryDeviceOrThrow(userId);

        // 查询根文件夹列表
        List<Folder> folders = folderRepository.findByUserIdAndPrimaryDeviceIdAndParentFolderIdIsNullAndStatus(
                userId, primaryDeviceId, FolderStatus.ACTIVE
        );

        // 转换为响应对象
        return folders.stream()
                .map(folderMapper::toResponse)
                .toList();
    }

    @Override
    public List<FolderResponse> getChildFolders(UUID parentFolderId, UUID userId) {
        // 如果 parentFolderId 为 null，返回根文件夹
        if (parentFolderId == null) {
            return getRootFolders(userId);
        }

        // 验证父文件夹存在且有权限访问
        validationService.getActiveFolderOrThrow(parentFolderId, userId);

        // 查询子文件夹列表
        List<Folder> folders = folderRepository.findByParentFolderIdAndUserIdAndStatus(
                parentFolderId, userId, FolderStatus.ACTIVE
        );

        // 转换为响应对象
        return folders.stream()
                .map(folderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public FolderResponse renameFolder(UUID folderId, String newName, UUID userId) {
        // 1. 获取并验证文件夹
        Folder folder = validationService.getActiveFolderOrThrow(folderId, userId);

        // 2. 检查新名称是否重复
        validationService.checkDuplicateFolderName(folder.getParentFolderId(), newName);

        // 3. 更新名称和路径
        String oldName = folder.getFolderName();
        String oldPath = folder.getFullPath();

        // 构建新路径：替换旧名称为新名称
        String newPath;
        if (folder.getParentFolderId() != null) {
            Folder parentFolder = validationService.getAndValidateParentFolder(folder.getParentFolderId(), userId);
            newPath = parentFolder.getFullPath() + "/" + newName;
        } else {
            newPath = "/" + newName;
        }

        folder.setFolderName(newName);
        folder.setFullPath(newPath);
        folderRepository.save(folder);

        log.info("文件夹重命名成功: folderId={}, userId={}, oldName={}, newName={}, oldPath={}, newPath={}",
                folderId, userId, oldName, newName, oldPath, newPath);

        return folderMapper.toResponse(folder);
    }

    @Override
    @Transactional
    public FolderResponse moveFolder(UUID folderId, UUID targetParentId, UUID userId) {
        // 1. 获取并验证要移动的文件夹
        Folder folder = validationService.getActiveFolderOrThrow(folderId, userId);

        // 2. 验证目标父文件夹
        Folder targetParent = validationService.getAndValidateParentFolder(targetParentId, userId);

        // 3. 检查目标位置是否有同名文件夹
        validationService.checkDuplicateFolderName(targetParentId, folder.getFolderName());

        // 4. 验证不能移动到自己的子孙文件夹中
        if (targetParentId != null) {
            Folder current = targetParent;
            while (current != null) {
                if (current.getFolderId().equals(folderId)) {
                    log.warn("不能将文件夹移动到其子文件夹中, folderId={}, targetParentId={}", folderId, targetParentId);
                    throw new FolderException(ErrorCode.FOLDER_MOVE_TO_DESCENDANT);
                }
                // 继续向上查找
                UUID parentId = current.getParentFolderId();
                current = parentId != null ? folderRepository.findByFolderId(parentId).orElse(null) : null;
            }
        }

        // 5. 计算新路径和层级
        String newFullPath = validationService.calculateFullPath(targetParent, folder.getFolderName());
        Integer newLevel = validationService.calculateLevel(targetParent);

        // 6. 更新文件夹信息
        UUID oldParentId = folder.getParentFolderId();
        String oldPath = folder.getFullPath();

        folder.setParentFolderId(targetParentId);
        folder.setFullPath(newFullPath);
        folder.setLevel(newLevel);
        folderRepository.save(folder);

        log.info("文件夹移动成功: folderId={}, userId={}, oldParentId={}, newParentId={}, oldPath={}, newPath={}",
                folderId, userId, oldParentId, targetParentId, oldPath, newFullPath);

        return folderMapper.toResponse(folder);
    }

    @Override
    @Transactional
    public FolderResponse updateMetadata(UUID folderId, FolderMetadataUpdateRequest request, UUID userId) {
        // 1. 获取并验证文件夹
        Folder folder = validationService.getActiveFolderOrThrow(folderId, userId);

        // 2. 部分更新元数据字段
        if (request.getColor() != null) {
            folder.setColor(request.getColor());
        }
        if (request.getIconName() != null) {
            folder.setIconName(request.getIconName());
        }
        if (request.getDescription() != null) {
            folder.setDescription(request.getDescription());
        }
        if (request.getIsStarred() != null) {
            folder.setIsStarred(request.getIsStarred());
        }
        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
        }

        folderRepository.save(folder);

        log.info("文件夹元数据更新成功: folderId={}, userId={}, updates={}", folderId, userId, request);

        return folderMapper.toResponse(folder);
    }

    @Override
    @Transactional
    public void deleteFolder(UUID folderId, UUID userId) {
        // 1. 获取并验证文件夹
        Folder folder = validationService.getFolderOrThrow(folderId, userId);

        // 2. 验证可以删除
        validationService.validateFolderDeletion(folder, false); // 允许删除非空文件夹

        // 3. 软删除文件夹
        folder.markAsDeleted();
        folderRepository.save(folder);

        log.info("文件夹删除成功: folderId={}, userId={}, fullPath={}", folderId, userId, folder.getFullPath());
    }

    @Override
    @Transactional
    public FolderResponse restoreFolder(UUID folderId, UUID userId) {
        // 1. 获取并验证文件夹
        Folder folder = validationService.getFolderOrThrow(folderId, userId);

        // 2. 检查文件夹状态
        if (!folder.isDeleted()) {
            log.warn("文件夹未处于删除状态，无法恢复: folderId={}, status={}", folderId, folder.getStatus());
            throw new FolderException(ErrorCode.FOLDER_NOT_FOUND);
        }

        // 3. 验证父文件夹是否存在且可用
        if (folder.getParentFolderId() != null) {
            Folder parentFolder = folderRepository.findByFolderId(folder.getParentFolderId()).orElse(null);
            if (parentFolder == null || !parentFolder.isActive()) {
                log.warn("父文件夹不存在或已删除，无法恢复: folderId={}, parentFolderId={}",
                        folderId, folder.getParentFolderId());
                throw new FolderException(ErrorCode.FOLDER_PARENT_NOT_FOUND);
            }
        }

        // 4. 恢复文件夹
        folder.restore();
        folderRepository.save(folder);

        log.info("文件夹恢复成功: folderId={}, userId={}, fullPath={}", folderId, userId, folder.getFullPath());

        return folderMapper.toResponse(folder);
    }

}
