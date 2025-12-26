package com.evorsio.mybox.storage.service;

import java.io.InputStream;

public interface MinioStorageService {
    void upload(String bucket, String objectName, InputStream inputStream, long size, String contentType);

    InputStream download(String bucket, String objectName);

    void delete(String bucket, String objectName);
}
