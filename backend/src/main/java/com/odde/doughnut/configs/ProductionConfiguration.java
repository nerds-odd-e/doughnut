package com.odde.doughnut.configs;

import com.odde.doughnut.configs.CommonConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Profile({"prod"})
public class ProductionConfiguration extends WebSecurityConfigurerAdapter {

    private final CommonConfiguration commonConfiguration = new CommonConfiguration();

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/api/healthcheck", "/api/bazaar_notes")
                .permitAll();

        commonConfiguration.commonConfig(http)
                .oauth2Login();
    }

}
