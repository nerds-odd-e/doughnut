package com.odde.doughnut.configs;

import com.odde.doughnut.services.openAiApis.ApiExecutor;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class OpenAiApiConfig {

  @Bean
  @SessionScope
  @Qualifier("testableOpenAiApi")
  public OpenAiApi getTestableOpenAiApi(@Value("${spring.openai.token}") String openAiToken, @Autowired TestabilitySettings testabilitySettings) {
    return ApiExecutor.getOpenAiApi(openAiToken, testabilitySettings.getOpenAiApiUrl());
  }
}
