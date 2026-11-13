package com.easy.user.service.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.easy.common.config.SmsConfig;
import com.easy.common.exception.ServiceException;
import com.easy.user.service.ISmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 阿里云短信发送：aliyun.sms.enabled=true 时生效
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aliyun.sms", name = "enabled", havingValue = "true")
public class AliyunSmsServiceImpl implements ISmsService {

    private final Client smsClient;
    private final SmsConfig smsConfig;

    @Override
    public void sendVerifyCode(String phone, String code) {
        try {
            String templateParam = "{\"code\":\"" + code + "\",\"min\":\"5\"}";

            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(smsConfig.getSignName())
                    .setTemplateCode(smsConfig.getTemplateCode())
                    .setTemplateParam(templateParam);

            SendSmsVerifyCodeResponse response = smsClient.sendSmsVerifyCode(request);
            String respCode = response.getBody().getCode();

            if (!"OK".equals(respCode)) {
                log.error("[SMS] 阿里云返回错误: phone={}, respCode={}, msg={}",
                        phone, respCode, response.getBody().getMessage());
                throw new ServiceException(500, "短信发送失败：" + response.getBody().getMessage());
            }
            log.info("[SMS] 阿里云发送成功: phone={}, bizId={}",
                    phone, response.getBody().getModel() != null
                            ? response.getBody().getModel().getBizId() : "null");

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[SMS] 阿里云调用异常: phone={}", phone, e);
            throw new ServiceException(500, "短信服务异常：" + e.getMessage());
        }
    }
}
