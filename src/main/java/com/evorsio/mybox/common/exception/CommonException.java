package com.evorsio.mybox.common.exception;

import com.evorsio.mybox.common.error.ErrorCode;
import lombok.Getter;
import org.springframework.modulith.NamedInterface;

@Getter
@NamedInterface("common.exception")
public abstract class CommonException extends RuntimeException {
    private final ErrorCode errorCode;

    protected CommonException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
