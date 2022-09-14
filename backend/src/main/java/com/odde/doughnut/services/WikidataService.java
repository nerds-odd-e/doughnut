package com.odde.doughnut.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.json.WikidataEntity;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.services.externalApis.WikidataEntityModel;
import com.odde.doughnut.services.externalApis.WikidataInfo;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataService(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
  private UriComponentsBuilder wikidataUriBuilder() {
    return UriComponentsBuilder.fromHttpUrl(wikidataUrl);
  }

  public WikidataEntity fetchWikidata(String wikidataId) throws IOException, InterruptedException {
    String responseBody = httpClientAdapter.getResponseString(ConstructWikidataUrl(wikidataId));
    if (responseBody == null) return null;
    WikidataModel wikidataModel =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    WikidataInfo wikidataInfo = wikidataModel.entities.get(wikidataId);
    return new WikidataEntity(
        wikidataInfo.GetEnglishTitle(), wikidataInfo.GetEnglishWikipediaUrl());
  }

  private URI ConstructWikidataUrl(String wikidataId) {
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

  public static class WikidataModel {
    public Map<String, WikidataInfo> entities;
  }

  public List<WikidataSearchEntity> fetchWikidataByQuery(String search)
      throws IOException, InterruptedException {
    URI uri =
        wikidataUriBuilder()
            .path("/w/api.php")
            .queryParam("action", "wbsearchentities")
            .queryParam("search", "{search}")
            .queryParam("format", "json")
            .queryParam("language", "en")
            .queryParam("uselang", "en")
            .queryParam("type", "item")
            .queryParam("limit", 10)
            .build(search);
    String responseBody = httpClientAdapter.getResponseString(uri);
    WikidataSearchModel entities =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    return entities.getWikidataSearchEntities();
  }

  public static class WikidataSearchModel {
    public List<Map<String, Object>> search;

    private List<WikidataSearchEntity> getWikidataSearchEntities() {
      return search.stream().map(WikidataSearchEntity::new).collect(Collectors.toList());
    }
  }

  @SneakyThrows
  public String getWikidataLocationDescription(String wikidataId) {
    WikidataEntityModel entity;
    try {
      entity = getEntityDataById(wikidataId);
    } catch (IOException e) {
      return null;
    }

    if (entity == null) return null;

    return entity.getLocationDescription(wikidataId);
  }

  private WikidataEntityModel getEntityDataById(String wikidataId)
      throws IOException, InterruptedException {
    URI uri =
        wikidataUriBuilder()
            .path("/w/api.php")
            .queryParam("action", "wbgetentities")
            .queryParam("ids", "{id}")
            .queryParam("format", "json")
            .queryParam("props", "claims")
            .build(wikidataId);
    String responseBody = httpClientAdapter.getResponseString(uri);
    if (responseBody == null) return null;
    return getObjectMapper().readValue(responseBody, new TypeReference<>() {});
  }
}
