package com.evorsio.mybox.file.service;

import com.evorsio.mybox.file.domain.File;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface FileService {
    File uploadFile(UUID ownerId, String originalFileName, long size, String contentType, InputStream inputStream);

    File getActiveFileById(UUID ownerId, UUID fileId);

    InputStream downloadFile(UUID ownerId, UUID fileId);

    void deleteFile(UUID ownerId, UUID fileId);

    List<File> listFiles(UUID ownerId);

    List<File> listDeletedFiles(UUID ownerId);

    void restoreFile(UUID ownerId, UUID fileId);
}
