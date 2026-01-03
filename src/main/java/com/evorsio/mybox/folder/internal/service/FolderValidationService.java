package com.evorsio.mybox.folder.internal.service;

import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.device.DeviceService;
import com.evorsio.mybox.folder.Folder;
import com.evorsio.mybox.folder.FolderStatus;
import com.evorsio.mybox.folder.internal.exception.FolderException;
import com.evorsio.mybox.folder.internal.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 文件夹业务验证和通用逻辑服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FolderValidationService {

    private final FolderRepository folderRepository;
    private final DeviceService deviceService;

    /**
     * 获取用户主设备 ID，如果不存在则抛出异常
     *
     * @param userId 用户 ID
     * @return 主设备 ID
     * @throws FolderException 如果用户没有主设备
     */
    public UUID getPrimaryDeviceOrThrow(UUID userId) {
        UUID primaryDeviceId = deviceService.getPrimaryDeviceId(userId);
        if (primaryDeviceId == null) {
            log.warn("用户没有主设备, userId={}", userId);
            throw new FolderException(ErrorCode.NO_PRIMARY_DEVICE);
        }
        return primaryDeviceId;
    }

    /**
     * 获取并验证父文件夹
     * <p>
     * 验证项：
     * 1. 父文件夹是否存在
     * 2. 父文件夹是否属于当前用户
     * 3. 父文件夹状态是否为 ACTIVE
     *
     * @param parentFolderId 父文件夹 ID（可为 null）
     * @param userId         当前用户 ID
     * @return 父文件夹对象，如果 parentFolderId 为 null 则返回 null
     * @throws FolderException 如果验证失败
     */
    public Folder getAndValidateParentFolder(UUID parentFolderId, UUID userId) {
        if (parentFolderId == null) {
            return null;
        }

        Folder parentFolder = folderRepository.findByFolderId(parentFolderId)
                .orElse(null);

        if (parentFolder == null) {
            log.warn("父文件夹不存在, parentFolderId={}", parentFolderId);
            throw new FolderException(ErrorCode.FOLDER_PARENT_NOT_FOUND);
        }

        if (!parentFolder.getUserId().equals(userId)) {
            log.warn("无权访问父文件夹, parentFolderId={}, userId={}", parentFolderId, userId);
            throw new FolderException(ErrorCode.FOLDER_PARENT_NOT_FOUND);
        }

        if (!parentFolder.isActive()) {
            log.warn("父文件夹状态异常, parentFolderId={}, status={}",
                    parentFolderId, parentFolder.getStatus());
            throw new FolderException(ErrorCode.FOLDER_PARENT_NOT_FOUND);
        }

        return parentFolder;
    }

    /**
     * 检查指定父文件夹下是否存在同名文件夹
     *
     * @param parentFolderId 父文件夹的业务 ID（UUID，可为 null）
     * @param folderName     文件夹名称
     * @throws FolderException 如果已存在同名文件夹
     */
    public void checkDuplicateFolderName(UUID parentFolderId, String folderName) {
        boolean exists = folderRepository.existsByParentFolderIdAndFolderNameAndStatusNot(
                parentFolderId,
                folderName,
                FolderStatus.DELETED
        );

        if (exists) {
            log.warn("文件夹名称重复, parentId={}, folderName={}", parentFolderId, folderName);
            throw new FolderException(ErrorCode.FOLDER_NAME_DUPLICATE);
        }
    }

    /**
     * 计算文件夹的完整路径
     *
     * @param parentFolder 父文件夹（可为 null）
     * @param folderName   当前文件夹名称
     * @return 完整路径，如 "/Documents/Work/Project"
     */
    public String calculateFullPath(Folder parentFolder, String folderName) {
        if (parentFolder != null) {
            return parentFolder.getFullPath() + "/" + folderName;
        }
        return "/" + folderName;
    }

    /**
     * 计算文件夹的层级深度
     *
     * @param parentFolder 父文件夹（可为 null）
     * @return 层级深度，根目录为 0
     */
    public Integer calculateLevel(Folder parentFolder) {
        if (parentFolder != null) {
            return parentFolder.getLevel() + 1;
        }
        return 0;
    }

    /**
     * 获取并验证文件夹是否存在
     *
     * @param folderId 文件夹 ID
     * @param userId   当前用户 ID
     * @return 文件夹对象
     * @throws FolderException 如果文件夹不存在或无权访问
     */
    public Folder getFolderOrThrow(UUID folderId, UUID userId) {
        Folder folder = folderRepository.findByFolderId(folderId)
                .orElse(null);

        if (folder == null) {
            log.warn("文件夹不存在, folderId={}", folderId);
            throw new FolderException(ErrorCode.FOLDER_NOT_FOUND);
        }

        if (!folder.getUserId().equals(userId)) {
            log.warn("无权访问文件夹, folderId={}, userId={}", folderId, userId);
            throw new FolderException(ErrorCode.FOLDER_NOT_FOUND);
        }

        return folder;
    }

    /**
     * 获取并验证文件夹是否存在且状态为 ACTIVE
     *
     * @param folderId 文件夹 ID
     * @param userId   当前用户 ID
     * @return 文件夹对象
     * @throws FolderException 如果文件夹不存在、无权访问或状态不是 ACTIVE
     */
    public Folder getActiveFolderOrThrow(UUID folderId, UUID userId) {
        Folder folder = getFolderOrThrow(folderId, userId);

        if (!folder.isActive()) {
            log.warn("文件夹状态异常, folderId={}, status={}", folderId, folder.getStatus());
            throw new FolderException(ErrorCode.FOLDER_NOT_FOUND);
        }

        return folder;
    }

    /**
     * 检查文件夹是否为系统文件夹
     *
     * @param folder 文件夹对象
     * @throws FolderException 如果是系统文件夹
     */
    public void checkNotSystemFolder(Folder folder) {
        if (folder.isSystemFolder()) {
            log.warn("系统文件夹不允许此操作, folderId={}", folder.getFolderId());
            throw new FolderException(ErrorCode.FOLDER_IS_SYSTEM_FOLDER);
        }
    }

    /**
     * 检查文件夹是否为空
     *
     * @param folder 文件夹对象
     * @throws FolderException 如果文件夹不为空
     */
    public void checkFolderEmpty(Folder folder) {
        if (!folder.isEmpty()) {
            log.warn("文件夹不为空, folderId={}, fileCount={}, folderCount={}",
                    folder.getFolderId(), folder.getFileCount(), folder.getFolderCount());
            throw new FolderException(ErrorCode.FOLDER_NOT_EMPTY);
        }
    }

    /**
     * 验证文件夹是否可以被删除
     * <p>
     * 检查项：
     * 1. 不是系统文件夹
     * 2. 文件夹为空（可选）
     *
     * @param folder        文件夹对象
     * @param requireEmpty  是否要求文件夹必须为空
     * @throws FolderException 如果验证失败
     */
    public void validateFolderDeletion(Folder folder, boolean requireEmpty) {
        checkNotSystemFolder(folder);
        if (requireEmpty) {
            checkFolderEmpty(folder);
        }
    }
}
