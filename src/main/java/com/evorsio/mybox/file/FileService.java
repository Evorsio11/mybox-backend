package com.evorsio.mybox.file;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface FileService {
    File uploadFile(UUID ownerId, String originalFileName, long size, String contentType, InputStream inputStream);

    File getActiveFileById(UUID ownerId, UUID fileId);

    InputStream downloadFile(UUID ownerId, UUID fileId);

    InputStream downloadPartialFile(UUID ownerId, UUID fileId, long start, long end);

    void deleteFile(UUID ownerId, UUID fileId);

    List<File> listFiles(UUID ownerId);

    List<File> listDeletedFiles(UUID ownerId);

    void restoreFile(UUID ownerId, UUID fileId);
}
