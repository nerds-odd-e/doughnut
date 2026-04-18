package com.odde.doughnut.configs;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

  private final CacheControl indexHtmlCacheControl;

  MvcConfig(Environment env) {
    if (env.acceptsProfiles(Profiles.of("prod"))) {
      this.indexHtmlCacheControl = CacheControl.maxAge(5, TimeUnit.MINUTES);
    } else {
      this.indexHtmlCacheControl = CacheControl.noStore();
    }
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/index.html")
        .addResourceLocations("classpath:/static/")
        .setCacheControl(indexHtmlCacheControl);
  }
}
