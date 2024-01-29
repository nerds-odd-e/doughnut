package com.odde.doughnut.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile({"e2e", "test"})
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class NonProductConfiguration {

  @Autowired private CommonConfiguration commonConfiguration;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf()
        .disable()
        .authorizeHttpRequests()
        .requestMatchers("/api/healthcheck", "/api/testability/**", "/api-docs/**", "/api-docs.json", "/api-docs.yaml")
        .permitAll()
        .and()
        .rememberMe()
        .alwaysRemember(true);

    commonConfiguration.commonConfig(http, http.httpBasic().and().formLogin());
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) throws Exception {
    InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
    createUser(manager, "user", passwordEncoder.encode("password"), "USER");
    createUser(manager, "old_learner", passwordEncoder.encode("password"), "USER");
    createUser(manager, "another_old_learner", passwordEncoder.encode("password"), "USER");
    createUser(manager, "manual", passwordEncoder.encode("password"), "USER");
    createUser(manager, "admin", passwordEncoder.encode("password"), "USER");
    createUser(manager, "non_admin", passwordEncoder.encode("password"), "USER");
    return manager;
  }

  private void createUser(
      InMemoryUserDetailsManager manager, String userName, String password, String role) {
    manager.createUser(User.withUsername(userName).password(password).roles(role).build());
  }
}
