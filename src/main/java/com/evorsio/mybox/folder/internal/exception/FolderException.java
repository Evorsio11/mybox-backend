package com.evorsio.mybox.folder.internal.exception;

import com.evorsio.mybox.common.CommonException;
import com.evorsio.mybox.common.ErrorCode;

public class FolderException extends CommonException {
    public FolderException(ErrorCode errorCode) {
        super(errorCode);
    }
}