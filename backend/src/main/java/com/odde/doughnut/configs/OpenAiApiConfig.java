package com.odde.doughnut.configs;

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
 * Configuration for OpenAI API client.
 *
 * <p>This configuration provides the official OpenAI Java SDK (com.openai:openai-java). The client
 * is configured to use the base URL from {@link TestabilitySettings}, allowing the {@link
 * com.odde.doughnut.testability.TestabilityRestController#replaceServiceUrl} endpoint to control
 * the client for testing purposes.
 */
@Configuration
public class OpenAiApiConfig {

  // Official OpenAI Java SDK - Non-prod: keep session scope so web/session features can swap
  // endpoints
  @Bean
  @Profile("!prod")
  @SessionScope
  @Qualifier("officialOpenAiClient")
  public OpenAIClient getOfficialOpenAiClientNonProd(
      @Value("${spring.openai.token}") String openAiToken,
      @Autowired TestabilitySettings testabilitySettings) {
    String baseUrl = testabilitySettings.getOpenAiApiUrl();
    // Use default URL if service is disabled (empty URL) to allow bean creation
    // Services check isOpenAiDisabled() before using the client
    if (baseUrl == null || baseUrl.isEmpty()) {
      baseUrl = "https://api.openai.com/v1/";
    }
    return OpenAIOkHttpClient.builder().apiKey(openAiToken).baseUrl(baseUrl).build();
  }

  // Official OpenAI Java SDK - Prod: provide the same qualified bean without session scope for
  // schedulers/background jobs
  @Bean
  @Profile("prod")
  @Qualifier("officialOpenAiClient")
  public OpenAIClient getOfficialOpenAiClientProd(
      @Value("${spring.openai.token}") String openAiToken,
      @Autowired TestabilitySettings testabilitySettings) {
    String baseUrl = testabilitySettings.getOpenAiApiUrl();
    // Use default URL if service is disabled (empty URL) to allow bean creation
    // Services check isOpenAiDisabled() before using the client
    if (baseUrl == null || baseUrl.isEmpty()) {
      baseUrl = "https://api.openai.com/v1/";
    }
    return OpenAIOkHttpClient.builder().apiKey(openAiToken).baseUrl(baseUrl).build();
  }
}
