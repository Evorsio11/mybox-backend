package com.evorsio.mybox.auth.exception;

import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.common.exception.CommonException;

public class InvalidCredentialsException extends CommonException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
