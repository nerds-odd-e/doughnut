package com.odde.doughnut.configs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.hibernate7.Hibernate7Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfig {
  @Bean
  public JsonMapperBuilderCustomizer defaultViewInclusionForWebJson() {
    return builder -> builder.enable(tools.jackson.databind.MapperFeature.DEFAULT_VIEW_INCLUSION);
  }

  @Bean
  public JsonMapper objectMapper() {
    return JsonMapper.builder()
        .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .addModule(new JavaTimeModule())
        .addModule(new Hibernate7Module())
        .build();
  }
}
