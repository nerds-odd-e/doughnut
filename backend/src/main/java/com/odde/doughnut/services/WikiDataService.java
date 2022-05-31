package com.odde.doughnut.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.models.WikiDataModel;
import java.io.IOException;

public class WikiDataService {
  private final String wikidataBaseUrl = "https://www.wikidata.org";

  private final HttpClientAdapter httpClientAdapter;

  public WikiDataService(HttpClientAdapter httpClientAdapter) {
    this.httpClientAdapter = httpClientAdapter;
  }

  public WikiDataModel FetchWikiData(String wikiDataId) throws IOException, InterruptedException {
    String responseBody = httpClientAdapter.getResponseString(ConstructWikiDataUrl(wikiDataId));
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(responseBody, new TypeReference<WikiDataModel>() {});
  }

  private String ConstructWikiDataUrl(String wikiDataId) {
    return wikidataBaseUrl + "/wiki/Special:EntityData/" + wikiDataId + ".json";
  }
}
