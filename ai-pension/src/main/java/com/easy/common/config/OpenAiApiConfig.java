package com.easy.common.config;

import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

  //  火山方舟（Volcengine Ark）适配配置。
  //  Spring AI OpenAI starter 默认请求路径为 {base-url}/v1/chat/completions，
  //  而火山方舟的 OpenAI 兼容端点不带 /v1 前缀（https://ark.cn-beijing.volces.com/api/v3/chat/completions）。
  //  因此自定义 OpenAiApi，把 completionsPath / embeddingsPath 覆盖为方舟的路径。

@Configuration
public class OpenAiApiConfig {

    @Bean
    public OpenAiApi openAiApi(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(new SimpleApiKey(apiKey))
                .completionsPath("/chat/completions")   // 方舟无 /v1 前缀
                .embeddingsPath("/embeddings")           // 方舟 Embedding 端点
                .build();
    }
}
