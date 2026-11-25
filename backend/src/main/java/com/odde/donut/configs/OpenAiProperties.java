package com.odde.donut.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.openai")
public record OpenAiProperties(String token) {}
