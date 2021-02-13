package com.odde.doughnut;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@SpringBootApplication
public class DoughnutApplication extends WebSecurityConfigurerAdapter {

  public static void main(String[] args) {
    SpringApplication.run(DoughnutApplication.class, args);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf()
        .disable()
        .authorizeRequests()
        .antMatchers("/api/healthcheck")
        .permitAll();

    http.authorizeRequests(
            a
            -> a.antMatchers("/", "/login", "/error", "/webjars/**")
                   .permitAll()
                   .anyRequest()
                   .authenticated())
        .logout(l -> l.logoutSuccessUrl("/").permitAll())
//            .httpBasic();
        .exceptionHandling(
            e
            -> e.authenticationEntryPoint(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .oauth2Login();
  }
}
