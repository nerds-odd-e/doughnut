package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

public record QueryResult(String response) {

  public <T> Optional<T> mapToOptional(Class<T> tClass) throws JsonProcessingException {
    return Optional.ofNullable(mapToObject(tClass));
  }

  public <T> T mapToObject(Class<T> tClass) throws JsonProcessingException {
    if (response == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(response, tClass);
  }
}
