package com.odde.doughnut.configs;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Cache index.html for only 5 minutes
    registry
        .addResourceHandler("/index.html")
        .addResourceLocations("classpath:/static/")
        .setCacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES));

    // Cache all other static resources (JS/CSS/assets with hashes) for 1 year
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
  }
}
