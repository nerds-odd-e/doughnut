package com.odde.doughnut.configs;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Profile({"e2e", "test"})
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class NonProductConfiguration {

  @Autowired private CommonConfiguration commonConfiguration;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable);
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/users/identify", // in non-product env, we use frontend to identify user
                        "/api/games")
                    .permitAll())
        .rememberMe(rememberMe -> rememberMe.alwaysRemember(true));

    commonConfiguration.commonConfig(http.httpBasic(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) throws Exception {
    InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
    createUser(manager, "user", passwordEncoder.encode("password"));
    createUser(manager, "old_learner", passwordEncoder.encode("password"));
    createUser(manager, "another_old_learner", passwordEncoder.encode("password"));
    createUser(manager, "manual", passwordEncoder.encode("password"));
    createUser(manager, "admin", passwordEncoder.encode("password"));
    createUser(manager, "non_admin", passwordEncoder.encode("password"));
    createUser(manager, "a_trainer", passwordEncoder.encode("password"));
    return manager;
  }

  private void createUser(InMemoryUserDetailsManager manager, String userName, String password) {
    manager.createUser(User.withUsername(userName).password(password).roles("USER").build());
  }
}
