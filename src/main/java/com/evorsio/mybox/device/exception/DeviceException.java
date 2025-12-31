package com.evorsio.mybox.device.exception;

import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.common.exception.CommonException;

public class DeviceException extends CommonException {
    public DeviceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
