package com.odde.donut.services.httpQuery;

import com.odde.donut.configs.ObjectMapperConfig;
import java.util.Optional;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public record QueryResult(String response) {
  private static final JsonMapper mapper =
      ObjectMapperConfig.handWrittenJsonMapper()
          .rebuild()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  public <T> Optional<T> mapToOptional(Class<T> tClass) {
    return Optional.ofNullable(mapToObject(tClass));
  }

  public <T> T mapToObject(Class<T> tClass) {
    if (response == null) return null;
    return mapper.readValue(response, tClass);
  }
}
