package com.odde.donut.configs;

import com.odde.donut.testability.TestabilitySettings;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Configuration for OpenAI API client.
 *
 * <p>This configuration provides the official OpenAI Java SDK (com.openai:openai-java). The client
 * is configured to use the base URL from {@link TestabilitySettings}, allowing the {@link
 * com.odde.donut.testability.TestabilityRestController#replaceServiceUrl} endpoint to control the
 * client for testing purposes.
 */
@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiApiConfig {

  // Official OpenAI Java SDK - Non-prod: request scope so each request reads the current
  // testability endpoint. No Spring destroy callback: closing at request end would kill async
  // streaming calls still in flight; the SDK closes the client when it becomes unreachable.
  @Bean(destroyMethod = "")
  @Profile("!prod")
  @RequestScope
  @Qualifier("officialOpenAiClient")
  public OpenAIClient getOfficialOpenAiClientNonProd(
      OpenAiProperties openAiProperties, @Autowired TestabilitySettings testabilitySettings) {
    String baseUrl = testabilitySettings.getOpenAiApiUrl();
    if (baseUrl == null || baseUrl.isEmpty()) {
      baseUrl = "https://api.openai.com/v1/";
    }
    return OpenAIOkHttpClient.builder().apiKey(openAiProperties.token()).baseUrl(baseUrl).build();
  }

  // Official OpenAI Java SDK - Prod: provide the same qualified bean without request scope for
  // schedulers/background jobs
  @Bean
  @Profile("prod")
  @Qualifier("officialOpenAiClient")
  public OpenAIClient getOfficialOpenAiClientProd(
      OpenAiProperties openAiProperties, @Autowired TestabilitySettings testabilitySettings) {
    String baseUrl = testabilitySettings.getOpenAiApiUrl();
    if (baseUrl == null || baseUrl.isEmpty()) {
      baseUrl = "https://api.openai.com/v1/";
    }
    return OpenAIOkHttpClient.builder().apiKey(openAiProperties.token()).baseUrl(baseUrl).build();
  }
}
