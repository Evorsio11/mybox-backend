package com.evorsio.mybox.common.exception;

import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Map<String, ErrorCode> REQUIRED_FIELD_MAP = Map.of(
            "username", ErrorCode.USERNAME_REQUIRED,
            "email", ErrorCode.EMAIL_REQUIRED,
            "password", ErrorCode.PASSWORD_REQUIRED
    );
    private static final Map<String, ErrorCode> FORMAT_ERROR_MAP = Map.of(
            "username", ErrorCode.USERNAME_FORMAT_INVALID,
            "email", ErrorCode.EMAIL_FORMAT_INVALID,
            "password", ErrorCode.PASSWORD_FORMAT_INVALID
    );


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError == null) {
            return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                    .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR.getCode(),
                            ErrorCode.VALIDATION_ERROR.getMessage()));
        }

        String field = fieldError.getField();
        String code = fieldError.getCode();

        ErrorCode errorCode;
        if ("NotBlank".equals(code)) {
            errorCode = REQUIRED_FIELD_MAP.getOrDefault(field, ErrorCode.VALIDATION_ERROR);
        } else {
            errorCode = FORMAT_ERROR_MAP.getOrDefault(field, ErrorCode.VALIDATION_ERROR);
        }
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ErrorResponse> handleCommon(CommonException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOthers(Exception e) {
        log.error("系统异常:", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(new ErrorResponse(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
