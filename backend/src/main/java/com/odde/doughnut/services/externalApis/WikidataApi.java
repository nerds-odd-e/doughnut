package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.odde.doughnut.services.HttpClientAdapter;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataApi(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
  private UriComponentsBuilder wikidataUriBuilder() {
    return UriComponentsBuilder.fromHttpUrl(wikidataUrl);
  }

  private URI constructWikidataUrl(String wikidataId) {
    return wikidataUriBuilder()
        .path("/wiki/Special:EntityData/" + wikidataId + ".json")
        .build()
        .toUri();
  }

  private ObjectMapper getObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  private String queryWikidataApi(String action, Function<UriComponentsBuilder, URI> uriBuilder)
      throws IOException, InterruptedException {
    URI uri =
        uriBuilder.apply(wikidataUriBuilder().path("/w/api.php").queryParam("action", action));
    return httpClientAdapter.getResponseString(uri);
  }

  public Optional<WikidataEntityDataHash> fetchWikidataEntityDataHash(String wikidataId)
      throws IOException, InterruptedException {
    String responseBody = httpClientAdapter.getResponseString(constructWikidataUrl(wikidataId));
    if (Strings.isEmpty(responseBody)) {
      return Optional.empty();
    }
    return Optional.of(getObjectMapper().readValue(responseBody, new TypeReference<>() {}));
  }

  public WikidataSearchModel getWikidataSearchEntities(String search)
      throws IOException, InterruptedException {
    String responseBody =
        queryWikidataApi(
            "wbsearchentities",
            (uriComponentsBuilder ->
                uriComponentsBuilder
                    .queryParam("search", "{search}")
                    .queryParam("format", "json")
                    .queryParam("language", "en")
                    .queryParam("uselang", "en")
                    .queryParam("type", "item")
                    .queryParam("limit", 10)
                    .build(search)));
    return getObjectMapper().readValue(responseBody, new TypeReference<>() {});
  }

  public WikidataEntityHash getEntityHashById(String wikidataId)
      throws IOException, InterruptedException {
    String responseBody =
        queryWikidataApi(
            "wbgetentities",
            (builder) ->
                builder
                    .queryParam("ids", "{id}")
                    .queryParam("format", "json")
                    .queryParam("props", "claims")
                    .build(wikidataId));
    if (responseBody == null) return null;
    try {
      return getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    } catch (MismatchedInputException e) {
      return null;
    }
  }
}
