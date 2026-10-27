package com.odde.donut.oauth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration
@EnableConfigurationProperties(AssistantClientProperties.class)
public class AssistantAuthorizationConfiguration {
  public static final String READ_SCOPE = "notes:read";

  @Bean
  RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcRegisteredClientRepository(jdbcTemplate);
  }

  @Bean
  OAuth2AuthorizationService oauth2AuthorizationService(
      JdbcTemplate jdbcTemplate, RegisteredClientRepository clients) {
    return new JdbcOAuth2AuthorizationService(jdbcTemplate, clients);
  }

  @Bean
  OAuth2AuthorizationConsentService oauth2AuthorizationConsentService(
      JdbcTemplate jdbcTemplate, RegisteredClientRepository clients) {
    return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, clients);
  }
}
