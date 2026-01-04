package com.evorsio.mybox.file.internal.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.evorsio.mybox.file.MinioStorageService;

import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        long startTime = System.currentTimeMillis();
        try {
            log.debug("[MinIO Client] 开始上传分片: bucket={}, objectName={}, size={} bytes",
                    bucket, objectName, size);

            long bucketCheckStart = System.currentTimeMillis();
            ensureBucketExists(bucket);
            log.debug("[MinIO Client] Bucket检查耗时: {}ms", System.currentTimeMillis() - bucketCheckStart);

            long putObjectStart = System.currentTimeMillis();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            long totalTime = System.currentTimeMillis() - startTime;
            long putObjectTime = System.currentTimeMillis() - putObjectStart;

            log.info("[MinIO Client] 上传分片成功: bucket={}, objectName={}, size={} bytes, " +
                    "putObject耗时={}ms, 总耗时={}ms",
                    bucket, objectName, size, putObjectTime, totalTime);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[MinIO Client] 上传分片失败: bucket={}, objectName={}, 耗时={}ms, error={}",
                    bucket, objectName, totalTime, e.getMessage(), e);
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

    @Override
    public void copyObject(String sourceBucket, String sourceObjectName, String targetBucket, String targetObjectName) {
        try {
            ensureBucketExists(targetBucket);
            
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(targetBucket)
                            .object(targetObjectName)
                            .source(
                                    CopySource.builder()
                                            .bucket(sourceBucket)
                                            .object(sourceObjectName)
                                            .build()
                            )
                            .build()
            );
            
            log.info("复制对象成功: {}:{} -> {}:{}", sourceBucket, sourceObjectName, targetBucket, targetObjectName);
        } catch (Exception e) {
            log.error("复制对象失败: {}:{} -> {}:{}", sourceBucket, sourceObjectName, targetBucket, targetObjectName, e);
            throw new RuntimeException("复制对象失败", e);
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
