package com.odde.doughnut.configs;

import com.odde.doughnut.configs.CommonConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;

@Configuration
@Profile({"prod"})
public class ProductionConfiguration extends WebSecurityConfigurerAdapter {

  private final CommonConfiguration commonConfiguration =
      new CommonConfiguration();

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http.csrf()
        .disable()
        .authorizeRequests()
        .antMatchers("/api/blog/*", "/api/healthcheck")
        .permitAll();

    commonConfiguration.commonConfig(http, http.oauth2Login());
  }

}
