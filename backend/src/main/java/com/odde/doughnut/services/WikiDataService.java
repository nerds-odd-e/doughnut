package com.odde.doughnut.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.json.WikiDataSearchResponseModel;
import com.odde.doughnut.models.WikiDataModel;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WikiDataService {
  private final String wikidataBaseUrl = "https://www.wikidata.org";
  public HttpClientAdapter httpClientAdapter = new HttpClientAdapter();

  public WikiDataModel FetchWikiData(String wikiDataId) throws IOException, InterruptedException {
    String responseBody = CallWikiDataApi(wikiDataId);
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(responseBody, new TypeReference<WikiDataModel>() {});
  }

  public List<WikiDataSearchResponseModel> searchWikiData(String searchTerm)
      throws IOException, InterruptedException {
    String response = httpClientAdapter.getResponseString(ConstructSearchWikiDataUrl(searchTerm));
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Object searchResult =
        mapper.readValue(response, new TypeReference<Map<String, Object>>() {}).get("search");
    return mapper.convertValue(searchResult, new TypeReference<>() {});
  }

  private String CallWikiDataApi(String wikiDataId) throws IOException, InterruptedException {
    String url = ConstructWikiDataUrl(wikiDataId);
    return httpClientAdapter.getResponseString(url);
  }

  private String ConstructWikiDataUrl(String wikiDataId) {
    return wikidataBaseUrl + "/wiki/Special:EntityData/" + wikiDataId + ".json";
  }

  private String ConstructSearchWikiDataUrl(String searchTerm) {
    String searchUrl =
        wikidataBaseUrl
            + "/w/api.php?action=wbsearchentities&search="
            + searchTerm
            + "&format=json&errorformat=plaintext&language=en&uselang=en&type=item";
    return searchUrl;
  }
}
