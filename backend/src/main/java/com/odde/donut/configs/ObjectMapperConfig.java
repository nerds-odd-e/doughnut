package com.odde.donut.configs;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.hibernate7.Hibernate7Module;
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
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .addModule(
                new Hibernate7Module()
                    .enable(Feature.FORCE_LAZY_LOADING)
                    .disable(Feature.USE_TRANSIENT_ANNOTATION));
  }

  /**
   * Mapper for hand-written serialization (stored JSON, MCP and AI payloads): declared property
   * order, nulls omitted (also inside maps), ISO dates, unknown properties refused. Not a bean: a
   * {@code JsonMapper} bean would replace Boot's web mapper.
   */
  public static JsonMapper handWrittenJsonMapper() {
    return JsonMapper.builderWithJackson2Defaults()
        .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .changeDefaultPropertyInclusion(inclusion ->
            inclusion
                .withValueInclusion(JsonInclude.Include.NON_NULL)
                .withContentInclusion(JsonInclude.Include.NON_NULL))
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
