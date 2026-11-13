package com.easy.common.exception;

import lombok.Data;

@Data
public class ServiceException extends RuntimeException{  //自定义业务异常类，携带业务状态码，用于在Service层抛出可预期的业务错误，由全局异常处理器统一捕获并返回前端。

    int code;

    public ServiceException(int code, String message){
        super(message);
        this.code=code;
    }

}
