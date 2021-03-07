package com.odde.doughnut;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public class CommonConfiguration {
    HttpSecurity commonConfig(HttpSecurity http) throws Exception {
        return http.authorizeRequests()
                .antMatchers("/", "/login", "/error", "/js/**", "/webjars/**", "/bazaar", "/bazaar/**/**")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/").invalidateHttpSession(true).permitAll());
    }
}