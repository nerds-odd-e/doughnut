package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.WikidataSearchEntity;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.services.httpQuery.QueryBuilder;
import com.odde.doughnut.services.wikidataApis.*;
import java.io.IOException;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataService(WikidataApi wikidataApi, TimestampService timestampService) {
  public WikidataService(
      HttpClientAdapter httpClientAdapter, String wikidataUrl, TimestampService timestampService) {
    this(
        new WikidataApi(
            new QueryBuilder(httpClientAdapter, UriComponentsBuilder.fromHttpUrl(wikidataUrl))),
        timestampService);
  }

  public List<WikidataSearchEntity> searchWikidata(String search)
      throws IOException, InterruptedException {
    return wikidataApi.getWikidataSearchEntities(search).getWikidataSearchEntities();
  }

  public WikidataIdWithApi wrapWikidataIdWithApi(String wikidataId) {
    return new WikidataId(wikidataId).withApi(wikidataApi, timestampService);
  }
}
