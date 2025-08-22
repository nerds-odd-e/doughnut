package com.odde.doughnut.configs;

import com.odde.doughnut.services.openAiApis.ApiExecutor;
import com.odde.doughnut.services.openAiApis.OpenAiApiExtended;
import com.odde.doughnut.testability.TestabilitySettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class OpenAiApiConfig {

  // Non-prod: keep session scope so web/session features can swap endpoints
  @Bean
  @Profile("!prod")
  @SessionScope
  @Qualifier("testableOpenAiApi")
  public OpenAiApiExtended getTestableOpenAiApiNonProd(
      @Value("${spring.openai.token}") String openAiToken,
      @Autowired TestabilitySettings testabilitySettings) {
    return ApiExecutor.getOpenAiApi(openAiToken, testabilitySettings.getOpenAiApiUrl());
  }

  // Prod: provide the same qualified bean without session scope for schedulers/background jobs
  @Bean
  @Profile("prod")
  @Qualifier("testableOpenAiApi")
  public OpenAiApiExtended getTestableOpenAiApiProd(
      @Value("${spring.openai.token}") String openAiToken,
      @Autowired TestabilitySettings testabilitySettings) {
    return ApiExecutor.getOpenAiApi(openAiToken, testabilitySettings.getOpenAiApiUrl());
  }
}
