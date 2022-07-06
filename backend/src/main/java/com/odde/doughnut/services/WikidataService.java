package com.odde.doughnut.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.WikidataEntity;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataService(
    HttpClientAdapter httpClientAdapter, UriComponentsBuilder wikidataUriBuilder) {
  public WikidataEntity fetchWikiData(String wikiDataId) throws IOException, InterruptedException {
    String responseBody = httpClientAdapter.getResponseString(ConstructWikiDataUrl(wikiDataId));
    WikiDataModel wikiDataModel =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    WikiDataInfo wikiDataInfo = wikiDataModel.entities.get(wikiDataId);
    return new WikidataEntity(
        wikiDataInfo.GetEnglishTitle(), wikiDataInfo.GetEnglishWikipediaUrl());
  }

  private String ConstructWikiDataUrl(String wikiDataId) {
    return wikidataUriBuilder
        .path("/wiki/Special:EntityData/" + wikiDataId + ".json")
        .toUriString();
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
      } catch (IOException e) {
        BindingResult bindingResult = new BeanPropertyBindingResult(wikidataId, "wikiDataId");
        bindingResult.rejectValue(null, "error.error", "The wikidata service is not available");
        throw new BindException(bindingResult);
      }
    }
  }

  public static class WikiDataModel {
    public Map<String, WikiDataInfo> entities;
  }

  public List<WikidataSearchEntity> fetchWikidataByQuery(String search)
      throws IOException, InterruptedException {
    String url =
        wikidataUriBuilder
            .path("/w/api.php")
            .queryParam("action", "wbsearchentities")
            .queryParam("search", URLEncoder.encode(search, StandardCharsets.UTF_8))
            .queryParam("format", "json")
            .queryParam("language", "en")
            .queryParam("uselang", "en")
            .queryParam("type", "item")
            .queryParam("limit", 10)
            .build()
            .toUriString();
    String responseBody = httpClientAdapter.getResponseString(url);
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
}
