package com.easy.common.annotation;

import java.lang.annotation.*;


// 自定义操作日志注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    String module() default "";

    String operation() default "";

    int operateType() default 0;
}