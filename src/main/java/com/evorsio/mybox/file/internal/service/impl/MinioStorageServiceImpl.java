package com.evorsio.mybox.file.internal.service.impl;

import com.evorsio.mybox.file.MinioStorageService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
    public InputStream downloadPartial(String bucket, String objectName, long offset, long length) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .offset(offset)
                            .length(length)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("下载部分文件失败", e);
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

    @Override
    public boolean exists(String bucket, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            return true; // 如果不抛出异常，说明对象存在
        } catch (Exception e) {
            return false; // 如果抛出异常，说明对象不存在
        }
    }

    // 分片上传相关方法实现

    @Override
    public void uploadChunk(String bucket, String objectName, InputStream inputStream, long size, String contentType) {
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
            throw new RuntimeException("上传分片失败", e);
        }
    }

    @Override
    public InputStream downloadChunk(String bucket, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("下载分片失败", e);
        }
    }

    @Override
    public void deleteChunks(String bucket, List<String> objectNames) {
        try {
            for (String objectName : objectNames) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("批量删除分片失败", e);
        }
    }

    @Override
    public void mergeChunks(String bucket, List<String> chunkObjectNames, String targetObjectName) {
        try {
            ensureBucketExists(bucket);

            // 构建分片源列表
            List<ComposeSource> sources = new ArrayList<>();
            for (String chunkObjectName : chunkObjectNames) {
                sources.add(
                        ComposeSource.builder()
                                .bucket(bucket)
                                .object(chunkObjectName)
                                .build()
                );
            }

            // 使用 MinIO 的对象合成功能合并分片
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket)
                            .object(targetObjectName)
                            .sources(sources)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("合并分片失败", e);
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
