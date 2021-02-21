package com.odde.doughnut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorAppConfig implements WebMvcConfigurer {
  @Autowired
  CurrentUserInterceptor currentUserInterceptor;

  @Override
  public void addInterceptors (InterceptorRegistry registry) {
    registry.addInterceptor(currentUserInterceptor);
  }
}

