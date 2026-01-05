package com.evorsio.mybox.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 统一错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Auth 1000
    USERNAME_ALREADY_EXISTS("AUTH_1001", "用户名已存在", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("AUTH_1002", "邮箱已存在", HttpStatus.CONFLICT),
    USER_NOT_FOUND("AUTH_1003", "用户未找到", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("AUTH_1004", "用户凭据错误", HttpStatus.UNAUTHORIZED),
    MISSING_AUTH_HEADER("AUTH_1005", "缺少 Authorization 头", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN("AUTH_1006", "令牌无效或已过期", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_1007", "权限不足", HttpStatus.FORBIDDEN),

    // 设备 1500
    DEVICE_FINGERPRINT_MISMATCH("DEVICE_1501", "设备信息验证失败，请重新登录", HttpStatus.FORBIDDEN),
    DEVICE_NOT_FOUND("DEVICE_1502", "设备未找到", HttpStatus.NOT_FOUND),
    DEVICE_ALREADY_DELETED("DEVICE_1503", "设备已被删除", HttpStatus.BAD_REQUEST),
    DEVICE_NOT_ACTIVE("DEVICE_1504", "设备未激活或被禁用", HttpStatus.FORBIDDEN),
    DEVICE_OFFLINE("DEVICE_1505", "设备离线", HttpStatus.CONFLICT),


    // 参数校验 1100
    VALIDATION_ERROR("AUTH_1100", "参数校验失败", HttpStatus.BAD_REQUEST),

    // 限流 1200
    RATE_LIMIT_EXCEEDED("AUTH_1200", "请求过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS),

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

    // 文件夹 3000
    FOLDER_NOT_FOUND("FOLDER_3001", "文件夹不存在或已删除", HttpStatus.NOT_FOUND),
    FOLDER_ALREADY_EXISTS("FOLDER_3002", "文件夹已存在", HttpStatus.CONFLICT),
    FOLDER_NAME_DUPLICATE("FOLDER_3003", "文件夹名称重复", HttpStatus.CONFLICT),
    FOLDER_CREATE_FAILED("FOLDER_3004", "文件夹创建失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FOLDER_DELETE_FAILED("FOLDER_3005", "文件夹删除失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FOLDER_UPDATE_FAILED("FOLDER_3006", "文件夹更新失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FOLDER_MOVE_FAILED("FOLDER_3007", "文件夹移动失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FOLDER_RENAME_FAILED("FOLDER_3008", "文件夹重命名失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FOLDER_RESTORE_FAILED("FOLDER_3009", "文件夹恢复失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FOLDER_ALREADY_DELETED("FOLDER_3010", "文件夹已被删除", HttpStatus.BAD_REQUEST),
    FOLDER_NOT_EMPTY("FOLDER_3011", "文件夹不为空，无法删除", HttpStatus.CONFLICT),
    FOLDER_IS_SYSTEM_FOLDER("FOLDER_3012", "系统文件夹不允许此操作", HttpStatus.FORBIDDEN),
    FOLDER_HAS_CHILDREN("FOLDER_3013", "文件夹包含子项，无法移动或删除", HttpStatus.CONFLICT),
    FOLDER_PARENT_NOT_FOUND("FOLDER_3014", "父文件夹不存在", HttpStatus.NOT_FOUND),
    FOLDER_MOVE_TO_DESCENDANT("FOLDER_3015", "不能将文件夹移动到其子文件夹中", HttpStatus.BAD_REQUEST),
    FOLDER_PATH_TOO_LONG("FOLDER_3016", "文件夹路径过长", HttpStatus.BAD_REQUEST),
    FOLDER_DEPTH_EXCEEDED("FOLDER_3017", "文件夹层级深度超过限制", HttpStatus.BAD_REQUEST),
    FOLDER_COUNT_EXCEEDED("FOLDER_3018", "文件夹数量超过限制", HttpStatus.CONFLICT),
    FOLDER_CANNOT_DELETE_ROOT("FOLDER_3019", "不能删除根文件夹", HttpStatus.FORBIDDEN),
    NO_PRIMARY_DEVICE("FOLDER_3020", "未找到主设备，请先创建设备", HttpStatus.BAD_REQUEST),

    // 系统错误 9000
    INTERNAL_ERROR("SYS_9000", "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
