package com.odde.doughnut.configs;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class CommonConfiguration {

  void commonConfig(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/users/identify").authenticated().anyRequest().permitAll())
        .logout(
            l ->
                l.logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .permitAll());
  }
}
