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
     * 错误响应（带 code）
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(
                false,
                code,
                message,
                null,
                null,
                Instant.now().getEpochSecond()
        );
    }

    /**
     * 错误响应（带 code 和 details）
     */
    public static <T> ApiResponse<T> error(String code, String message, List<FieldError> details) {
        ErrorDetail errorDetail = new ErrorDetail(details);
        return new ApiResponse<>(
                false,
                code,
                message,
                null,
                errorDetail,
                Instant.now().getEpochSecond()
        );
    }
}
