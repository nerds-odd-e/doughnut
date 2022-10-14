package com.odde.doughnut.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"e2e", "test"})
public class NonProductConfiguration extends WebSecurityConfigurerAdapter {

  @Autowired private CommonConfiguration commonConfiguration;

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    auth.inMemoryAuthentication()
        .withUser("user")
        .password(encoder.encode("password"))
        .roles("USER")
        .and()
        .withUser("old_learner")
        .password(encoder.encode("password"))
        .roles("USER")
        .and()
        .withUser("another_old_learner")
        .password(encoder.encode("password"))
        .roles("USER")
        .and()
        .withUser("manual")
        .password(encoder.encode("password"))
        .roles("USER")
        .and()
        .withUser("developer")
        .password(encoder.encode("password"))
        .roles("USER")
        .and()
        .withUser("non_developer")
        .password(encoder.encode("password"))
        .roles("USER");
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http.csrf()
        .disable()
        .authorizeRequests()
        .antMatchers("/api/healthcheck", "/api/testability/**")
        .permitAll();

    commonConfiguration.commonConfig(http, http.httpBasic().and().formLogin());
  }
}
