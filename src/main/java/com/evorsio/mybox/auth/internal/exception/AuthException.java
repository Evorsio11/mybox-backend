package com.evorsio.mybox.auth.internal.exception;

import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.common.CommonException;

public class AuthException extends CommonException {
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
