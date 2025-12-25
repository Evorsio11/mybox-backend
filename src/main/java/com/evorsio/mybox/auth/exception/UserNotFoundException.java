package com.evorsio.mybox.auth.exception;

import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.common.exception.CommonException;

public class UserNotFoundException extends CommonException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
