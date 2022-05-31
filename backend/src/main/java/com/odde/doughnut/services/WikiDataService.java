package com.odde.doughnut.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.json.WikiDataDto;
import com.odde.doughnut.models.WikiDataModel;
import java.io.IOException;

public class WikiDataService {
  private final String wikidataBaseUrl;

  private final HttpClientAdapter httpClientAdapter;

  public WikiDataService(HttpClientAdapter httpClientAdapter, String wikidataBaseUrl) {
    this.httpClientAdapter = httpClientAdapter;
    this.wikidataBaseUrl = wikidataBaseUrl;
  }

  private WikiDataModel FetchWikiData(String wikiDataId) throws IOException, InterruptedException {
    String responseBody = httpClientAdapter.getResponseString(ConstructWikiDataUrl(wikiDataId));
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(responseBody, new TypeReference<WikiDataModel>() {});
  }

  private String ConstructWikiDataUrl(String wikiDataId) {
    return wikidataBaseUrl + "/wiki/Special:EntityData/" + wikiDataId + ".json";
  }

  public WikiDataDto fetchWikiData(String wikiDataId) throws IOException, InterruptedException {
    return FetchWikiData(wikiDataId).GetInfoForWikiDataId(wikiDataId).processInfo();
  }
}
