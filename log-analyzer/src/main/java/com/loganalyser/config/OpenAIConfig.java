package com.loganalyser.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAIConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-4}")
    private String model;

    @Value("${openai.max-tokens:2000}")
    private Integer maxTokens;

    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(apiKey, Duration.ofSeconds(60));
    }

    public String getModel() {
        return model;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }
}

