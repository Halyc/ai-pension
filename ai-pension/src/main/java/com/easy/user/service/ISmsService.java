package com.easy.user.service;

/**
 * 短信服务：负责把验证码真正发送到用户手机
 */
public interface ISmsService {

    void sendVerifyCode(String phone, String code);
}
