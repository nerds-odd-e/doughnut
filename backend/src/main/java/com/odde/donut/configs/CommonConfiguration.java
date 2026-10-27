package com.odde.donut.configs;

import com.odde.donut.oauth.AssistantAccessTokenResolver;
import com.odde.donut.oauth.AssistantOAuthEndpoints;
import com.odde.donut.services.UserService;
import com.odde.donut.testability.TestAccessTokenResolver;
import java.util.Optional;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class CommonConfiguration {

  @Bean
  FilterRegistrationBean<ApiBearerAuthenticationFilter> apiBearerAuthenticationFilter(
      UserService users,
      AssistantAccessTokenResolver assistantTokens,
      AssistantOAuthEndpoints endpoints,
      Optional<TestAccessTokenResolver> testTokens) {
    var registration =
        new FilterRegistrationBean<>(
            new ApiBearerAuthenticationFilter(users, assistantTokens, endpoints, testTokens));
    registration.addUrlPatterns("/api/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
  }

  void oauth2Login(HttpSecurity http) throws Exception {
    http.oauth2Login(Customizer.withDefaults());
  }

  void commonConfig(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/login/continue").authenticated().anyRequest().permitAll())
        .logout(
            l ->
                l.logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .permitAll());
  }
}
