package com.evorsio.mybox.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 统一API响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private ErrorDetail error;
    private long timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private List<FieldError> details;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private String field;
        private String message;
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                "SUCCESS",
                "操作成功",
                data,
                null,
                Instant.now().getEpochSecond()
        );
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 成功响应（仅消息，无数据）
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(
                true,
                "SUCCESS",
                message,
                null,
                null,
                Instant.now().getEpochSecond()
        );
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                "SUCCESS",
                message,
                data,
                null,
                Instant.now().getEpochSecond()
        );
    }

    /**
     * 创建错误响应（仅用于全局异常处理器）
     * @param errorCode 错误码枚举
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                null,
                Instant.now().getEpochSecond()
        );
    }

    /**
     * 创建错误响应（仅用于全局异常处理器）
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage) {
        return new ApiResponse<>(
                false,
                errorCode.getCode(),
                customMessage,
                null,
                null,
                Instant.now().getEpochSecond()
        );
    }

    /**
     * 创建错误响应（仅用于全局异常处理器，带字段校验详情）
     * @param errorCode 错误码枚举
     * @param details 字段校验错误详情
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, List<FieldError> details) {
        ErrorDetail errorDetail = new ErrorDetail(details);
        return new ApiResponse<>(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                errorDetail,
                Instant.now().getEpochSecond()
        );
    }
}
