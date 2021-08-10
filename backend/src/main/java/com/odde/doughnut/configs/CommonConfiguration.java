package com.odde.doughnut.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

public class CommonConfiguration {
  void commonConfig(HttpSecurity http, AbstractAuthenticationFilterConfigurer authenticationFilterConfigurer) throws Exception {
    HttpSecurity config =  http.authorizeRequests()
        .mvcMatchers("/robots.txt")
        .permitAll()
        .antMatchers("/", "/login", "/error", "/images/**", "/odd-e.png",
                     "/webjars/**", "/assets/**","/bazaar", "/bazaar/**/**", "/blog/**/**",
                     "/api/**"
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
    authenticationFilterConfigurer.successHandler(myAuthenticationSuccessHandler());
  }

    @Bean
    public AuthenticationSuccessHandler myAuthenticationSuccessHandler(){
        return new MySimpleUrlAuthenticationSuccessHandler();
    }
}
