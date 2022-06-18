package com.odde.doughnut.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.WikidataEntity;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.util.Strings;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;

public record WikidataService(HttpClientAdapter httpClientAdapter, String wikidataBaseUrl) {

  public WikidataEntity fetchWikiData(String wikiDataId) throws IOException, InterruptedException {
    String responseBody = httpClientAdapter.getResponseString(ConstructWikiDataUrl(wikiDataId));
    WikiDataModel wikiDataModel =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    WikiDataInfo wikiDataInfo = wikiDataModel.entities.get(wikiDataId);
    return new WikidataEntity(
        wikiDataInfo.GetEnglishTitle(), wikiDataInfo.GetEnglishWikipediaUrl());
  }

  private String ConstructWikiDataUrl(String wikiDataId) {
    return wikidataBaseUrl + "/wiki/Special:EntityData/" + wikiDataId + ".json";
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

  public ArrayList<WikidataSearchEntity> fetchWikidataByQuery(String search)
      throws IOException, InterruptedException {
    String url =
        wikidataBaseUrl
            + "/w/api.php?action=wbsearchentities&search="
            + search
            + "&format=json&errorformat=plaintext&language=en&uselang=en&type=item&limit=10";
    String responseBody = httpClientAdapter.getResponseString(url);
    WikidataSearchModel entities =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    ArrayList<WikidataSearchEntity> myArray = new ArrayList<WikidataSearchEntity>();
    for (Map<String, Object> object : (List<Map<String, Object>>) entities.search) {
      WikidataSearchEntity item = new WikidataSearchEntity((Map<String, Object>) object);
      myArray.add(item);
    }
    return myArray;
  }

  public static class WikidataSearchModel {
    public List<Map<String, Object>> search;
  }
}
