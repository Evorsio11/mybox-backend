package com.evorsio.mybox.folder.internal.repository;

import com.evorsio.mybox.folder.Folder;
import com.evorsio.mybox.folder.FolderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 文件夹 Repository
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Integer> {

    /**
     * 根据 folderId（UUID）查询文件夹
     */
    Optional<Folder> findByFolderId(UUID folderId);

    /**
     * 查询指定父文件夹下是否存在同名文件夹（排除已删除状态）
     * 注意：parentFolderId 是业务 UUID，不是数据库主键
     */
    boolean existsByParentFolderIdAndFolderNameAndStatusNot(UUID parentFolderId, String folderName, FolderStatus status);

    /**
     * 查询用户的根文件夹列表（parentFolderId 为 null）
     */
    List<Folder> findByUserIdAndPrimaryDeviceIdAndParentFolderIdIsNullAndStatus(
            UUID userId,
            UUID primaryDeviceId,
            FolderStatus status
    );

    /**
     * 查询指定父文件夹的直接子文件夹列表
     *
     * @param parentFolderId 父文件夹的业务 ID（UUID）
     * @param userId         用户 ID
     * @param status         文件夹状态
     */
    List<Folder> findByParentFolderIdAndUserIdAndStatus(
            UUID parentFolderId,
            UUID userId,
            FolderStatus status
    );
}
