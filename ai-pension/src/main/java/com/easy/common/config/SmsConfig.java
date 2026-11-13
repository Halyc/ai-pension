package com.easy.common.config;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信（号码认证服务）配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.sms")
@ConditionalOnProperty(prefix = "aliyun.sms", name = "enabled", havingValue = "true")
public class SmsConfig {

    private String accessKey;
    private String secretKey;
    private String endpoint = "dypnsapi.aliyuncs.com";
    private String signName;
    private String templateCode;
    private boolean enabled = false;

    @Bean
    public Client smsClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKey)
                .setAccessKeySecret(secretKey)
                .setEndpoint(endpoint);
        return new Client(config);
    }
}
