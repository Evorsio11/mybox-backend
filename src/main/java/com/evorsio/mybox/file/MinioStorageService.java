package com.evorsio.mybox.file;

import java.io.InputStream;

public interface MinioStorageService {
    void upload(String bucket, String objectName, InputStream inputStream, long size, String contentType);

    InputStream download(String bucket, String objectName);

    InputStream downloadPartial(String bucket, String objectName, long offset, long length);

    void delete(String bucket, String objectName);

    boolean exists(String bucket, String objectName);

    // 分片上传相关方法
    void uploadChunk(String bucket, String objectName, InputStream inputStream, long size, String contentType);

    InputStream downloadChunk(String bucket, String objectName);

    void deleteChunks(String bucket, java.util.List<String> objectNames);

    void mergeChunks(String bucket, java.util.List<String> chunkObjectNames, String targetObjectName);
}
