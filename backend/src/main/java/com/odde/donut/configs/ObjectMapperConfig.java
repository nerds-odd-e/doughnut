package com.odde.donut.configs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.hibernate7.Hibernate7Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ObjectMapperConfig implements WebMvcConfigurer {
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

  /**
   * This API only ever serializes JSON with {@link #objectMapper()}. Without this, Spring Boot's
   * auto-configured XML converter (present only because a dependency pulls in
   * jackson-dataformat-xml transitively) wins content negotiation whenever a client's Accept header
   * prefers XML, e.g. a browser navigating to an API URL directly. That converter's own
   * ObjectMapper has no Hibernate7Module, so any lazily-loaded entity in the response fails to
   * serialize.
   */
  @Override
  public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
    builder.configureMessageConvertersList(
        converters -> converters.removeIf(ObjectMapperConfig::producesXml));
  }

  private static boolean producesXml(HttpMessageConverter<?> converter) {
    return converter.getSupportedMediaTypes().stream()
        .anyMatch(mediaType -> mediaType.getSubtype().contains("xml"));
  }
}
