package com.evorsio.mybox.auth.exception;

import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.common.exception.CommonException;

public class AuthException extends CommonException {
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
