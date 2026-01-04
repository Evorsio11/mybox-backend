package com.evorsio.mybox.file;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一分片上传请求（支持 Uppy XHRUpload）
 * 单接口设计，自动处理初始化、上传和合并
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChunkUploadRequest {
    /**
     * 文件分片数据
     */
    @NotNull(message = "文件不能为空")
    private MultipartFile file;

    /**
     * 原始文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String originalFileName;

    /**
     * 文件总大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    private Long fileSize;

    /**
     * 文件 MIME 类型
     */
    @NotBlank(message = "文件类型不能为空")
    private String contentType;

    /**
     * 当前分片编号（从1开始）
     */
    @NotNull(message = "分片编号不能为空")
    @Positive(message = "分片编号必须大于0")
    private Integer chunkNumber;

    /**
     * 总分片数
     */
    @NotNull(message = "总分片数不能为空")
    @Positive(message = "总分片数必须大于0")
    private Integer totalChunks;

    /**
     * 文件唯一标识（用于关联同一个文件的多个分片）
     * 可以是文件名+大小的hash，或客户端生成的UUID
     */
    @NotBlank(message = "文件标识不能为空")
    private String fileIdentifier;
}
