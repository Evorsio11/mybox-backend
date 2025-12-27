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

    //必填字段
    USERNAME_REQUIRED("AUTH_1101", "用户名不能为空", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED("AUTH_1102", "邮件不能为空", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED("AUTH_1103", "密码不能为空", HttpStatus.BAD_REQUEST),

    // 文件 2000
    FILE_NOT_FOUND("FILE_2001", "文件不存在或已删除", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED("FILE_2002", "文件上传失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED("FILE_2003", "文件删除失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_RESTORE_FAILED("FILE_2004", "文件恢复失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DOWNLOAD_FAILED("FILE_2005", "文件下载失败", HttpStatus.INTERNAL_SERVER_ERROR),


    // File 参数校验
    OWNER_ID_REQUIRED("FILE_2101", "ownerId 不能为空", HttpStatus.BAD_REQUEST),
    FILE_ID_REQUIRED("FILE_2102", "fileId 不能为空", HttpStatus.BAD_REQUEST),
    FILE_REQUIRED("FILE_2103", "文件不能为空", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_ALLOWED("FILE_2104", "文件类型不允许", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("FILE_2006", "上传文件太大，超过允许的最大限制", HttpStatus.PAYLOAD_TOO_LARGE),

    // 格式错误
    USERNAME_FORMAT_INVALID("AUTH_1111", "用户名格式不正确", HttpStatus.BAD_REQUEST),
    EMAIL_FORMAT_INVALID("AUTH_1112", "邮箱格式不正确", HttpStatus.BAD_REQUEST),
    PASSWORD_FORMAT_INVALID("AUTH_1113", "密码格式不正确（长度6-20位，必须包含字母和数字）", HttpStatus.BAD_REQUEST),

    // 9000
    INTERNAL_ERROR("SYS_9000", "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
