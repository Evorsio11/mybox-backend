package com.evorsio.mybox.file.internal.exception;

import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.common.CommonException;

public class FileException extends CommonException {
    public FileException(ErrorCode errorCode) {
        super(errorCode);
    }
}
