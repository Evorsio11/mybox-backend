package com.evorsio.mybox.file;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface FileService {
    File uploadFile(UUID ownerId, String originalFileName, long size, String contentType, InputStream inputStream);

    /**
     * 上传文件到指定文件夹
     */
    File uploadFile(UUID ownerId, UUID folderId, String originalFileName, long size, String contentType, InputStream inputStream);

    File getActiveFileById(UUID ownerId, UUID fileId);

    InputStream downloadFile(UUID ownerId, UUID fileId);

    InputStream downloadPartialFile(UUID ownerId, UUID fileId, long start, long end);

    void deleteFile(UUID ownerId, UUID fileId);

    List<File> listFiles(UUID ownerId);

    List<File> listDeletedFiles(UUID ownerId);

    void restoreFile(UUID ownerId, UUID fileId);

    /**
     * 获取指定文件夹内的文件列表
     */
    List<File> listFilesByFolder(UUID ownerId, UUID folderId);

    /**
     * 获取未分类文件列表（folderId 为 null）
     */
    List<File> listUnclassifiedFiles(UUID ownerId);

    /**
     * 移动文件到指定文件夹
     */
    File moveFileToFolder(UUID ownerId, UUID fileId, UUID targetFolderId);
}
