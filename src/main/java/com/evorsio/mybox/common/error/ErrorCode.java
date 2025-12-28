package com.evorsio.mybox.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Auth 1000
    USERNAME_ALREADY_EXISTS("AUTH_1001", "用户名已存在", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("AUTH_1002", "邮箱已存在", HttpStatus.CONFLICT),
    USER_NOT_FOUND("AUTH_1003", "用户未找到", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("AUTH_1004", "用户凭据错误", HttpStatus.UNAUTHORIZED),

    // 参数校验 1100
    VALIDATION_ERROR("AUTH_1100", "参数校验失败", HttpStatus.BAD_REQUEST),

    // 文件 2000
    FILE_NOT_FOUND("FILE_2001", "文件不存在或已删除", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED("FILE_2002", "文件上传失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED("FILE_2003", "文件删除失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_RESTORE_FAILED("FILE_2004", "文件恢复失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DOWNLOAD_FAILED("FILE_2005", "文件下载失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TOO_LARGE("FILE_2006", "上传文件太大，超过允许的最大限制", HttpStatus.PAYLOAD_TOO_LARGE),
    STORAGE_FULL("FILE_2007", "储存空间已满，请调整最大空间", HttpStatus.PAYLOAD_TOO_LARGE),

    // File 业务验证
    FILE_TYPE_NOT_ALLOWED("FILE_2104", "文件类型不允许", HttpStatus.BAD_REQUEST),

    // 分片上传 2200
    CHUNK_UPLOAD_FAILED("FILE_2201", "分片上传失败", HttpStatus.INTERNAL_SERVER_ERROR),
    CHUNK_UPLOAD_INTERRUPTED("FILE_2202", "分片上传中断", HttpStatus.INTERNAL_SERVER_ERROR),
    UPLOAD_SESSION_NOT_FOUND("FILE_2203", "上传会话不存在", HttpStatus.NOT_FOUND),
    UPLOAD_SESSION_EXPIRED("FILE_2204", "上传会话已过期", HttpStatus.GONE),
    CHUNK_NUMBER_INVALID("FILE_2205", "分片编号无效", HttpStatus.BAD_REQUEST),
    CHUNK_MERGE_FAILED("FILE_2206", "分片合并失败", HttpStatus.INTERNAL_SERVER_ERROR),
    CHUNK_UPLOAD_INCOMPLETE("FILE_2207", "分片上传不完整", HttpStatus.BAD_REQUEST),
    CONCURRENT_UPLOAD_LIMIT_EXCEEDED("FILE_2208", "超过并发上传限制", HttpStatus.TOO_MANY_REQUESTS),
    FILE_TOO_LARGE_FOR_CHUNK("FILE_2209", "文件过大，超过分片上传限制", HttpStatus.PAYLOAD_TOO_LARGE),
    CHUNK_UPLOAD_DISABLED("FILE_2210", "分片上传功能未启用", HttpStatus.FORBIDDEN),

    // 系统错误 9000
    INTERNAL_ERROR("SYS_9000", "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
