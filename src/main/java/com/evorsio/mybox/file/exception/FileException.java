package com.evorsio.mybox.file.exception;

import com.evorsio.mybox.api.error.ErrorCode;
import com.evorsio.mybox.common.exception.CommonException;

public class FileException extends CommonException {
    public FileException(ErrorCode errorCode) {
        super(errorCode);
    }
}
