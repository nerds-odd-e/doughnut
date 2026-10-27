package com.odde.donut.configs;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class CommonConfiguration {

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
