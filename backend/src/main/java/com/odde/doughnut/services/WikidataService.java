package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.WikidataSearchEntity;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.services.httpQuery.QueryBuilder;
import com.odde.doughnut.services.wikidataApis.*;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WikidataService {
  private final WikidataApi wikidataApi;

  public WikidataService(
      HttpClientAdapter httpClientAdapter, TestabilitySettings testabilitySettings) {
    this.wikidataApi =
        new WikidataApi(
            new QueryBuilder(
                httpClientAdapter,
                UriComponentsBuilder.fromHttpUrl(testabilitySettings.getWikidataServiceUrl())));
  }

  public List<WikidataSearchEntity> searchWikidata(String search)
      throws IOException, InterruptedException {
    return wikidataApi.getWikidataSearchEntities(search).getWikidataSearchEntities();
  }

  public WikidataIdWithApi wrapWikidataIdWithApi(String wikidataId) {
    return new WikidataId(wikidataId).withApi(wikidataApi);
  }
}
