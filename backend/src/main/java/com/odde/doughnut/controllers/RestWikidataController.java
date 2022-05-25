package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.json.WikiDataDto;
import com.odde.doughnut.models.LanguageValueModel;
import com.odde.doughnut.models.WikiDataInfo;
import com.odde.doughnut.models.WikiDataModel;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestWikidataController {
  @GetMapping("/wikidata/{wikiDataId}")
  public WikiDataDto fetchWikiDataDto(@PathVariable("wikiDataId") String wikiDataId)
      throws IOException, InterruptedException {
    WikiDataModel wikiModel = fetchWikidata(wikiDataId);
    WikiDataDto returnDto = new WikiDataDto();
    if (wikiModel.entities.containsKey(wikiDataId)) {
      WikiDataInfo myInfo = wikiModel.entities.get(wikiDataId);
      LanguageValueModel englishTitlePair = myInfo.labels.get("en");
      returnDto.WikiDataId = wikiDataId;
      returnDto.WikiDataTitleInEnglish = englishTitlePair.value;
    }
    return returnDto;
  }

  private WikiDataModel fetchWikidata(String wikiDataId) throws IOException, InterruptedException {
    HttpResponse<String> response = CallWikiDataApi(wikiDataId);
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(response.body(), new TypeReference<>() {});
  }

  private HttpResponse<String> CallWikiDataApi(String wikiDataId)
      throws IOException, InterruptedException {
    String url = ConstructWikiDataUrl(wikiDataId);
    final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
    HttpRequest request = builder.build();
    HttpResponse.BodyHandler<String> bodyHandler =
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    return HttpClient.newBuilder().build().send(request, bodyHandler);
  }

  private String ConstructWikiDataUrl(String wikiDataId) {
    return "https://www.wikidata.org/wiki/Special:EntityData/" + wikiDataId + ".json";
  }
}
