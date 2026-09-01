package com.odde.donut.configs;

import com.odde.donut.algorithms.CanonicalDonutOrigin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CanonicalDonutOriginConfiguration {

  @Bean
  CanonicalDonutOrigin canonicalDonutOrigin(
      @Value("${donut.canonical-origin:" + CanonicalDonutOrigin.PRODUCTION_DEFAULT + "}")
          String origin) {
    return CanonicalDonutOrigin.parse(origin);
  }
}
