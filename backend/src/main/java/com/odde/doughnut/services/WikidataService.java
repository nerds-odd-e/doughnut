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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
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

  public void assignWikidataLocationDataToNote(Note note, String wikidataId) {
    if (Strings.isEmpty(wikidataId)) {
      return;
    }
    WikidataLocationModel locationData;
    locationData = getEntityLocationDataById(wikidataId);
    if (locationData != null) {
      String prevDesc =
          note.getTextContent().getDescription() != null
              ? note.getTextContent().getDescription()
              : "";
      String desc = locationData + "\n" + prevDesc;
      note.getTextContent().setDescription(desc);
    }
  }

  public WikidataLocationModel getEntityLocationDataById(String wikidataId) {
    final String locationId = "P625";
    WikidataEntityModel entity = null;
    try {
      entity = getEntityDataById(wikidataId);
    } catch (IOException e) {
    } catch (InterruptedException e) {

    }

    if (entity == null) return null;

    Map<String, Object> locationValue = entity.getStringObjectMap(wikidataId, locationId);
    if (locationValue == null) return null;

    return new WikidataLocationModel(
        locationValue.get("latitude").toString(), locationValue.get("longitude").toString());
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
    WikidataEntityModel entity =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    return entity;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class WikidataEntityModel {
    private Map<String, WikidataEntityItemModel> entities;
    private Number success;

    public List<WikidataEntityItemObjectModel> getLocationClaims(
        String wikidataId, String locationId) {
      WikidataEntityItemModel entityItem = getEntities().get(wikidataId);
      if (entityItem.getClaims() == null) {
        return null;
      }
      if (entityItem.getClaims().containsKey(locationId)) {
        return entityItem.getClaims().get(locationId);
      }

      return null;
    }

    public Map<String, Object> getStringObjectMap(String wikidataId, String locationId) {
      if (!getEntities().containsKey(wikidataId)) return null;
      List<WikidataEntityItemObjectModel> locationClaims =
          getLocationClaims(wikidataId, locationId);
      if (locationClaims == null) {
        return null;
      }
      Map<String, Object> locationValue = locationClaims.get(0).getData();
      return locationValue;
    }
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
    static String VALUE_TYPE_KEY = "type";

    static class VALUE_TYPE {
      public static String GLOBE_COORDINATE = "globecoordinate";
      public static String STRING = "string";
    }

    private String type;
    private String id;
    Map<String, Object> data;

    @JsonProperty("mainsnak")
    private void unpackNested(Map<String, JsonNode> mainsnak) {
      if (mainsnak.containsKey(DATAVALUE_KEY) && mainsnak.get(DATAVALUE_KEY).has(VALUE_KEY)) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode value = mainsnak.get(DATAVALUE_KEY);
        if (VALUE_TYPE.GLOBE_COORDINATE.compareToIgnoreCase(value.get(VALUE_TYPE_KEY).textValue())
            == 0) {
          data =
              mapper.convertValue(
                  mainsnak.get(DATAVALUE_KEY).get(VALUE_KEY),
                  new TypeReference<Map<String, Object>>() {});
        } else if (VALUE_TYPE.STRING.compareToIgnoreCase(value.get(VALUE_TYPE_KEY).textValue())
            == 0) {
          String stringValue =
              mapper.convertValue(
                  mainsnak.get(DATAVALUE_KEY).get(VALUE_KEY), new TypeReference<String>() {});
          data = new LinkedHashMap<>();
          data.put(VALUE_KEY, stringValue);
        }
      }
    }
  }
}
