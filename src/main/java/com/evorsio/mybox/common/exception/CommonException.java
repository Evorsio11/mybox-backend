package com.evorsio.mybox.common.exception;

import com.evorsio.mybox.api.error.ErrorCode;
import lombok.Getter;

@Getter
public abstract class CommonException extends RuntimeException {
    private final ErrorCode errorCode;

    protected CommonException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
