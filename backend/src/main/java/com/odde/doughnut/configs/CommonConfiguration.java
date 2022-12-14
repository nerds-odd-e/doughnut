package com.odde.doughnut.configs;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CommonConfiguration {

  void commonConfig(
      HttpSecurity http, AbstractAuthenticationFilterConfigurer authenticationFilterConfigurer)
      throws Exception {
    http.authorizeHttpRequests()
        .requestMatchers("/robots.txt")
        .permitAll()
        .requestMatchers(
            "/",
            "/login",
            "/error",
            "/images/**",
            "/odd-e.png",
            "/odd-e.ico",
            "/webjars/**",
            "/assets/**",
            "/bazaar",
            "/bazaar/**",
            "/api/**",
            "/circles/join")
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .logout(
            l ->
                l.logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .permitAll());
  }
}
