package com.evorsio.mybox.auth.exception;

import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.common.exception.CommonException;

public class UsernameAlreadyExistsException extends CommonException {
    public UsernameAlreadyExistsException() {
        super(ErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
