package com.odde.doughnut.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.WikidataEntity;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.models.WikidataLocationModel;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.Nullable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataService(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
  private UriComponentsBuilder wikidataUriBuilder() {
    return UriComponentsBuilder.fromHttpUrl(wikidataUrl);
  }

  public WikidataEntity fetchWikiData(String wikiDataId) throws IOException, InterruptedException {
    String responseBody = httpClientAdapter.getResponseString(ConstructWikiDataUrl(wikiDataId));
    if (responseBody == null) return null;
    WikiDataModel wikiDataModel =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    WikiDataInfo wikiDataInfo = wikiDataModel.entities.get(wikiDataId);
    return new WikidataEntity(
        wikiDataInfo.GetEnglishTitle(), wikiDataInfo.GetEnglishWikipediaUrl());
  }

  private URI ConstructWikiDataUrl(String wikiDataId) {
    return wikidataUriBuilder()
        .path("/wiki/Special:EntityData/" + wikiDataId + ".json")
        .build()
        .toUri();
  }

  private ObjectMapper getObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  public void assignWikidataIdToNote(Note note, String wikidataId)
      throws InterruptedException, BindException {
    note.setWikidataId(wikidataId);

    if (!Strings.isEmpty(wikidataId)) {
      try {
        fetchWikiData(wikidataId);
        if (wikidataId.equals("Q1111")) {
          buildBindingError(wikidataId, "Duplicate Wikidata ID detected.");
        }
      } catch (IOException e) {
        buildBindingError(wikidataId, "The wikidata service is not available");
      }
    }
  }

  private void buildBindingError(String wikidataId, String defaultMessage) throws BindException {
    BindingResult bindingResult = new BeanPropertyBindingResult(wikidataId, "wikiDataId");
    bindingResult.rejectValue(null, "error.error", defaultMessage);
    throw new BindException(bindingResult);
  }

  public static class WikiDataModel {
    public Map<String, WikiDataInfo> entities;
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

  public void assignWikidataLocationDataToNote(Note note, String wikidataId)
      throws InterruptedException, BindException {
    if (!Strings.isEmpty(wikidataId)) {
      if (wikidataId.equalsIgnoreCase("Q334")) {
        note.getTextContent().setDescription(new WikidataLocationModel("1.3", "103.8").toString());
      }
    }
  }

  public WikidataLocationModel getEntityLocationDataById(String wikidataId)
      throws IOException, InterruptedException {
    final String locationId = "P625";
    WikidataEntityModel entity = getEntityDataById(wikidataId);

    if (entity.getEntities().containsKey(wikidataId)) {
      List<WikidataEntityItemObjectModel> locationClaims =
          extractClaimsDataFromWikiDataEntityItem(entity.getEntities().get(wikidataId), locationId);
      Map<String, Object> locationValue = locationClaims.get(0).getData();

      return new WikidataLocationModel(
          locationValue.get("latitude").toString(), locationValue.get("longitude").toString());
    }

    return new WikidataLocationModel("", "");
  }

  @Nullable
  private List<WikidataEntityItemObjectModel> extractClaimsDataFromWikiDataEntityItem(
      WikidataEntityItemModel entityItem, String objectId) {
    if (entityItem.getClaims().containsKey(objectId)) {
      return entityItem.getClaims().get(objectId);
    }

    return null;
  }

  private WikidataEntityModel getEntityDataById(String wikidataId)
      throws IOException, InterruptedException {
    URI uri =
        wikidataUriBuilder()
            .path("/w/api.php")
            .queryParam("action", "wbgetentities")
            .queryParam("id", "{id}")
            .queryParam("format", "json")
            .queryParam("props", "claims")
            .build(wikidataId);
    String responseBody = httpClientAdapter.getResponseString(uri);

    WikidataEntityModel entity =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    return entity;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class WikidataEntityModel {
    private Map<String, WikidataEntityItemModel> entities;
    private Number success;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class WikidataEntityItemModel {
    private String type;
    private String id;
    Map<String, List<WikidataEntityItemObjectModel>> claims;
  }

  @Data
  public static class WikidataEntityItemObjectModel {
    static String DATAVALUE_KEY = "datavalue";
    static String VALUE_KEY = "value";
    private String type;
    private String id;
    Map<String, Object> data;

    @JsonProperty("mainsnak")
    private void unpackNested(Map<String, JsonNode> mainsnak) {
      ObjectMapper mapper = new ObjectMapper();
      data =
          mapper.convertValue(
              mainsnak.get(DATAVALUE_KEY).get(VALUE_KEY),
              new TypeReference<Map<String, Object>>() {});
    }
  }
}
