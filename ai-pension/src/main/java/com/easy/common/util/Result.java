package com.easy.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 统一返回结果类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor

// 统一返回结果封装类
public class Result {
    private int code;//状态码
    private String message;//返回消息，说明
    private Object data;

    public static Result success(){
        return new Result(200,"操作成功",null);
    }

    public static Result success(Object data){
        return new Result(200,"操作成功",data);
    }

    public static Result success(String message,Object data){
        return new Result(200,message,data);
    }

    public static Result success(int code,String message,Object data){
        return new Result(code,message,data);
    }

    public static Result error(){
        return new Result(500,"操作失败",null);
    }

    public static Result error(int code,String msg){
        return new Result(code,msg,null);
    }

    public static Result error500(String message){
        return new Result(500,message,null);
    }
}
