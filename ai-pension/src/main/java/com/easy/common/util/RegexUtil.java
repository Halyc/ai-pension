package com.easy.common.util;

//正则表达式工具类，提供手机号和密码格式的正则校验
public class RegexUtil {

    private static final String phone_regex = "^1\\d{10}$";
    private static final String password_regex = "^\\w{6,16}$";

    public static boolean phone(final String phone){
        return phone.matches(phone_regex);
    }

    public static boolean password(final String password){
        return password.matches(password_regex);
    }

}
