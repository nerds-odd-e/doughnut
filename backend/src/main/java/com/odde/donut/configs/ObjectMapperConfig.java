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
import tools.jackson.datatype.hibernate7.Hibernate7Module.Feature;

@Configuration
public class ObjectMapperConfig implements WebMvcConfigurer {
  /**
   * Web JSON (controller responses) is serialized by Spring Boot's auto-configured Jackson 3
   * mapper, customized here. Lazy Hibernate proxies are loaded and written as their values, never
   * as proxy internals; JPA {@code @Transient} members stay in the JSON.
   */
  @Bean
  public JsonMapperBuilderCustomizer webJson() {
    return builder ->
        builder
            .enable(tools.jackson.databind.MapperFeature.DEFAULT_VIEW_INCLUSION)
            .addModule(
                new tools.jackson.datatype.hibernate7.Hibernate7Module()
                    .enable(Feature.FORCE_LAZY_LOADING)
                    .disable(Feature.USE_TRANSIENT_ANNOTATION));
  }

  /** Jackson 2 mapper for hand-written serialization only; web responses do not use it. */
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
   * This API only ever serializes JSON, with the mapper customized by {@link #webJson()}. Without
   * this, Spring Boot's auto-configured XML converter (present only because a dependency pulls in
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
