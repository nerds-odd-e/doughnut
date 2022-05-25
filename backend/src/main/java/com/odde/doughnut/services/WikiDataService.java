package com.odde.doughnut.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.odde.doughnut.entities.json.WikiDataSearchResponseModel;
import com.odde.doughnut.models.WikiDataModel;
import com.odde.doughnut.models.WikiDataSearchResponse;
import java.io.IOException;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class WikiDataService {
  public HttpClientAdapter httpClientAdapter = new HttpClientAdapter();

  public WikiDataModel FetchWikiData(String wikiDataId) throws IOException, InterruptedException {
    String responseBody = CallWikiDataApi(wikiDataId);
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(responseBody, new TypeReference<>() {});
  }

  private String CallWikiDataApi(String wikiDataId) throws IOException, InterruptedException {
    String url = ConstructWikiDataUrl(wikiDataId);
    return httpClientAdapter.getResponseString(url);
  }

  private String ConstructWikiDataUrl(String wikiDataId) {
    return "https://www.wikidata.org/wiki/Special:EntityData/" + wikiDataId + ".json";
  }

  public List<WikiDataSearchResponseModel> searchWikiData(String searchTerm)
      throws IOException, InterruptedException {
    String response =
        httpClientAdapter.getResponseString(
            "https://www.wikidata.org/w/api.php?action=wbsearchentities&search="
                + searchTerm
                + "&format=json&errorformat=plaintext&language=en&uselang=en&type=item");
    Gson gson = new Gson();
    return gson.fromJson(response, WikiDataSearchResponse.class).search.stream().limit(10).toList();
  }
}
