package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.HttpClientAdapter;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataApi(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
  public HttpClientAdapter getHttpClientAdapter() {
    return httpClientAdapter;
  }

  public String getWikidataUrl() {
    return wikidataUrl;
  }

  public UriComponentsBuilder wikidataUriBuilder() {
    return UriComponentsBuilder.fromHttpUrl(getWikidataUrl());
  }

  public Optional<WikidataEntityDataHash> fetchWikidataEntityDataHash(String wikidataId)
      throws IOException, InterruptedException {
    String responseBody =
        getHttpClientAdapter().getResponseString(constructWikidataUrl(wikidataId));
    if (Strings.isEmpty(responseBody)) {
      return Optional.empty();
    }
    return Optional.of(getObjectMapper().readValue(responseBody, new TypeReference<>() {}));
  }

  private URI constructWikidataUrl(String wikidataId) {
    return wikidataUriBuilder()
        .path("/wiki/Special:EntityData/" + wikidataId + ".json")
        .build()
        .toUri();
  }

  public ObjectMapper getObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }
}
