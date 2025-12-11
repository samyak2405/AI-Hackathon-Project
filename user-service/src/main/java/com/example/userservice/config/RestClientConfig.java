package com.example.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        List<HttpMessageConverter<?>> converters = new ArrayList<>(template.getMessageConverters());
        converters.removeIf(c -> c instanceof StringHttpMessageConverter);
        converters.add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        template.setMessageConverters(converters);
        return template;
    }
}


