package com.easy.common.advice;

import com.easy.common.exception.ServiceException;
import com.easy.common.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//全局异常处理器的注解
@RestControllerAdvice  // 标注此类为全局异常处理组件，且所有方法返回值自动序列化为JSON
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = ServiceException.class)
    public Result exceptionHandler(ServiceException e){
        e.printStackTrace();
        int code = e.getCode();      //获取业务状态码
        String message = e.getMessage();  //异常对象中获取异常消息文本
        return Result.error(code,message);  //封装返回给前端

    }

    //兜底异常处理：捕获所有未被上面精确匹配的异常（NPE、SQL异常、文件超限等）
    @ExceptionHandler(value = Exception.class)
    public Result exceptionHandler(Exception e){
        log.error("系统异常", e);   //完整堆栈只记录到日志，不返回给前端
        return Result.error(500,"系统繁忙，请稍后重试");  //对外只返回友好提示
    }
}
//全局异常处理器，统一捕获Controller层抛出的 ServiceException 业务异常，
// 将其转换为统一的 Result 响应格式返回给前端，避免异常堆栈直接暴露给用户。