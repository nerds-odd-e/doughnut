package com.odde.doughnut.configs;

import java.util.stream.Stream;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CommonConfiguration {

  void commonConfig(
      HttpSecurity http, AbstractAuthenticationFilterConfigurer authenticationFilterConfigurer)
      throws Exception {

    String[] backendRoutes = {
      "/login",
      "/error",
      "/odd-e.png",
      "/odd-e.ico",
      "/webjars/**",
      "/images/**",
      "/assets/**",
      "/api/**"
    };

    // the following array has to be in sync with the frontend routes in ApplicationController.java
    // Because java annotation does not allow variable, we have to repeat the routes here.
    String[] frontendRoutes = {
      "/",
      "/bazaar/**",
      "/circles/**",
      "/notebooks/**",
      "/notes/**",
      "/reviews/**",
      "/answers/**",
      "/links/**",
      "/failure-report-list/**",
      "/admin-dashboard/**"
    };

    String[] allRoutes =
        Stream.concat(Stream.of(backendRoutes), Stream.of(frontendRoutes)).toArray(String[]::new);

    http.authorizeHttpRequests()
        .requestMatchers("/robots.txt")
        .permitAll()
        .requestMatchers(allRoutes)
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
