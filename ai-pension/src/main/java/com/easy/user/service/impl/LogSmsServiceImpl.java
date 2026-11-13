package com.easy.user.service.impl;

import com.easy.user.service.ISmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 模拟短信发送：aliyun.sms.enabled=false（默认）时生效，验证码只打印到后端日志，便于本地开发联调。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "aliyun.sms", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LogSmsServiceImpl implements ISmsService {

    @Override
    public void sendVerifyCode(String phone, String code) {
        log.info("[SMS-LOG] （模拟发送）phone={}, code={}", phone, code);
    }
}
