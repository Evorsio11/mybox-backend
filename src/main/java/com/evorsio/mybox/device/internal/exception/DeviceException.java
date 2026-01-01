package com.evorsio.mybox.device.internal.exception;

import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.common.CommonException;

public class DeviceException extends CommonException {
    public DeviceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
