package com.evorsio.mybox.file;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * 文件记录服务接口
 * <p>
 * 管理用户视角的文件记录，包括上传、下载、删除等操作
 */
public interface FileRecordService {
    FileRecord uploadFileRecord(UUID ownerId, String originalFileName, long size, String contentType, InputStream inputStream);

    /**
     * 上传文件记录到指定文件夹
     */
    FileRecord uploadFileRecord(UUID ownerId, UUID folderId, String originalFileName, long size, String contentType, InputStream inputStream);

    FileRecord getActiveFileRecordById(UUID ownerId, UUID fileRecordId);

    InputStream downloadFileRecord(UUID ownerId, UUID fileRecordId);

    InputStream downloadPartialFileRecord(UUID ownerId, UUID fileRecordId, long start, long end);

    void deleteFileRecord(UUID ownerId, UUID fileRecordId);

    List<FileRecord> listFileRecords(UUID ownerId);

    List<FileRecord> listDeletedFileRecords(UUID ownerId);

    void restoreFileRecord(UUID ownerId, UUID fileRecordId);

    /**
     * 获取指定文件夹内的文件记录列表
     */
    List<FileRecord> listFileRecordsByFolder(UUID ownerId, UUID folderId);

    /**
     * 获取未分类文件记录列表（folderId 为 null）
     */
    List<FileRecord> listUnclassifiedFileRecords(UUID ownerId);

    /**
     * 移动文件记录到指定文件夹
     */
    FileRecord moveFileRecordToFolder(UUID ownerId, UUID fileRecordId, UUID targetFolderId);
}
