package com.spingbootinit.common.exception;

import com.spingbootinit.common.result.ResultCode;
import lombok.Data;

@Data
public class IllegalArgumentException extends RuntimeException {
    private final Integer code;


    public IllegalArgumentException(String message, Integer code) {
        super(message);
        this.code = code;
    }
    public IllegalArgumentException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

}
