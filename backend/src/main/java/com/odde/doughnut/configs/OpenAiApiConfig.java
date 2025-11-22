package com.odde.doughnut.configs;

import com.odde.doughnut.services.openAiApis.ApiExecutor;
import com.odde.doughnut.services.openAiApis.OpenAiApiExtended;
import com.odde.doughnut.testability.TestabilitySettings;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Configuration for OpenAI API clients.
 *
 * <p>This configuration provides both the legacy client (io.github.lambdua:service) and the
 * official SDK (com.openai:openai-java). The legacy client is still needed for:
 *
 * <ul>
 *   <li>Fine-tuning jobs (API not available in official SDK 4.8.0)
 *   <li>Audio transcription (API not available in official SDK 4.8.0)
 *   <li>Model listing (not critical, can be migrated later)
 * </ul>
 *
 * <p>Both clients are configured to use the same base URL from {@link TestabilitySettings},
 * allowing the {@link com.odde.doughnut.testability.TestabilityRestController#replaceServiceUrl}
 * endpoint to control both clients for testing purposes.
 */
@Configuration
public class OpenAiApiConfig {

  // Legacy client - still needed for fine-tuning and transcription APIs
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

  // Legacy client - Prod: provide the same qualified bean without session scope for
  // schedulers/background jobs
  @Bean
  @Profile("prod")
  @Qualifier("testableOpenAiApi")
  public OpenAiApiExtended getTestableOpenAiApiProd(
      @Value("${spring.openai.token}") String openAiToken,
      @Autowired TestabilitySettings testabilitySettings) {
    return ApiExecutor.getOpenAiApi(openAiToken, testabilitySettings.getOpenAiApiUrl());
  }

  // Official OpenAI Java SDK - Non-prod: keep session scope so web/session features can swap
  // endpoints
  @Bean
  @Profile("!prod")
  @SessionScope
  @Qualifier("officialOpenAiClient")
  public OpenAIClient getOfficialOpenAiClientNonProd(
      @Value("${spring.openai.token}") String openAiToken,
      @Autowired TestabilitySettings testabilitySettings) {
    return OpenAIOkHttpClient.builder()
        .apiKey(openAiToken)
        .baseUrl(testabilitySettings.getOpenAiApiUrl())
        .build();
  }

  // Official OpenAI Java SDK - Prod: provide the same qualified bean without session scope for
  // schedulers/background jobs
  @Bean
  @Profile("prod")
  @Qualifier("officialOpenAiClient")
  public OpenAIClient getOfficialOpenAiClientProd(
      @Value("${spring.openai.token}") String openAiToken,
      @Autowired TestabilitySettings testabilitySettings) {
    return OpenAIOkHttpClient.builder()
        .apiKey(openAiToken)
        .baseUrl(testabilitySettings.getOpenAiApiUrl())
        .build();
  }
}
