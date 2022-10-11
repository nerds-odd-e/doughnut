package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WikidataObjectMapper {
  private final ObjectMapper mapper = new ObjectMapper();

  public WikidataObjectMapper() {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  WikidataSearchModel getWikidataSearchModel(String responseBody) throws JsonProcessingException {
    return mapper.readValue(responseBody, new TypeReference<>() {});
  }

  WikidataEntityHash getWikidataEntityHash(String responseBody) throws JsonProcessingException {
    return mapper.readValue(responseBody, new TypeReference<>() {});
  }

  WikidataEntityDataHash getWikidataEntityDataHash(String responseBody)
      throws JsonProcessingException {
    return mapper.readValue(responseBody, new TypeReference<>() {});
  }
}
