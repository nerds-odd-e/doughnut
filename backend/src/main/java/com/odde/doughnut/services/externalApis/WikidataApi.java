package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.QueryBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataApi(
    HttpClientAdapter httpClientAdapter, String wikidataUrl, QueryBuilder queryBuilder) {
  public WikidataApi(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
    this(httpClientAdapter, wikidataUrl, new QueryBuilder());
  }

  private UriComponentsBuilder wikidataUriBuilder() {
    return UriComponentsBuilder.fromHttpUrl(wikidataUrl);
  }

  private String queryWikidataApi(String action, Function<UriComponentsBuilder, URI> uriBuilder)
      throws IOException, InterruptedException {
    URI uri =
        uriBuilder.apply(wikidataUriBuilder().path("/w/api.php").queryParam("action", action));
    return httpClientAdapter.getResponseString(uri);
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
    return new WikidataObjectMapper().getWikidataSearchModel(responseBody);
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
      return new WikidataObjectMapper().getWikidataEntityHash(responseBody);
    } catch (MismatchedInputException e) {
      return null;
    }
  }

  @SneakyThrows
  public Optional<WikidataEntityData> getWikidataEntityData(String wikidataId) {
    URI uri =
        wikidataUriBuilder()
            .path("/wiki/Special:EntityData/" + wikidataId + ".json")
            .build()
            .toUri();
    String responseBody = httpClientAdapter.getResponseString(uri);
    if (Strings.isEmpty(responseBody)) {
      return Optional.empty();
    }
    WikidataEntityDataHash result =
        new WikidataObjectMapper().getWikidataEntityDataHash(responseBody);
    return Optional.of(result.getWikidataEntity(wikidataId));
  }
}
