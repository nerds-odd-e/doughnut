package com.odde.doughnut.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CommonConfiguration {
    @Autowired
    public AccountAwareUrlAuthenticationSuccessHandler accountAwareUrlAuthenticationSuccessHandler;

  void commonConfig(HttpSecurity http, AbstractAuthenticationFilterConfigurer authenticationFilterConfigurer) throws Exception {
    HttpSecurity config =  http.authorizeRequests()
        .mvcMatchers("/robots.txt")
        .permitAll()
        .antMatchers("/", "/login", "/error", "/images/**", "/odd-e.png",
                     "/webjars/**", "/assets/**","/bazaar", "/bazaar/**/**",
                     "/api/**", "/circles/join"
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
    authenticationFilterConfigurer.successHandler(accountAwareUrlAuthenticationSuccessHandler);
  }
}
