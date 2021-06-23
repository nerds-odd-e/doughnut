package com.odde.doughnut.configs;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public class CommonConfiguration {
  HttpSecurity commonConfig(HttpSecurity http) throws Exception {
    return http.authorizeRequests()
        .mvcMatchers("/robots.txt")
        .permitAll()
        .antMatchers("/", "/login", "/error", "/images/**", "/odd-e.png",
                     "/webjars/**", "/assets/**","/bazaar", "/bazaar/**/**", "/blog/**/**",
                     "/api/notes/**", "/api/review-points/**", "/api/backdoor",
                     "/api/reviews/**"
                )
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .logout(l
                -> l.logoutUrl("/logout")
                       .logoutSuccessUrl("/")
                       .invalidateHttpSession(true)
                       .permitAll());
  }
}
