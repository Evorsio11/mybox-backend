package com.evorsio.mybox.file.service.impl;

import com.evorsio.mybox.file.service.MinioStorageService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements MinioStorageService {
    private final MinioClient minioClient;

    @Override
    public void upload(String bucket, String objectName, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucketExists(bucket);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("上传文件失败", e);
        }
    }

    @Override
    public InputStream download(String bucket, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("下载文件失败", e);
        }
    }

    @Override
    public void delete(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("删除文件失败", e);
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucket)
                        .build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucket)
                            .build()
            );
        }
    }

}
