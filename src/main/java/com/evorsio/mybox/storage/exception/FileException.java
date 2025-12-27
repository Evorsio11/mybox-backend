package com.evorsio.mybox.storage.exception;

import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.common.exception.CommonException;

public class FileException extends CommonException {
    public FileException(ErrorCode errorCode) {
        super(errorCode);
    }
}
