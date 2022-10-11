package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

public record QueryResult(String response) {
  public <T> Optional<T> mapToObject(Class<T> tClass) throws JsonProcessingException {
    if (response == null) return Optional.empty();
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return Optional.of(mapper.readValue(response, tClass));
  }
}
