package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.services.QueryBuilder;
import java.io.IOException;
import java.util.Optional;
import lombok.SneakyThrows;

public record WikidataApi(QueryBuilder queryBuilder) {

  private QueryBuilder queryWikidataApi(String action) {
    return queryBuilder.path("/w/api.php").queryParam("action", action);
  }

  public WikidataSearchResult getWikidataSearchEntities(String search)
      throws IOException, InterruptedException {
    return queryWikidataApi("wbsearchentities")
        .queryParam("search", "{search}")
        .queryParam("format", "json")
        .queryParam("language", "en")
        .queryParam("uselang", "en")
        .queryParam("type", "item")
        .queryParam("limit", 10)
        .queryResult(search)
        .mapToObject(WikidataSearchResult.class);
  }

  public WikidataEntityHash getEntityHashById(String wikidataId)
      throws IOException, InterruptedException {
    try {
      return queryWikidataApi("wbgetentities")
          .queryParam("ids", wikidataId)
          .queryParam("format", "json")
          .queryParam("props", "claims")
          .queryResult()
          .mapToObject(WikidataEntityHash.class);
    } catch (MismatchedInputException e) {
      return null;
    }
  }

  @SneakyThrows
  public Optional<WikidataEntityData> getWikidataEntityData(String wikidataId) {
    return queryBuilder
        .path("/wiki/Special:EntityData/" + wikidataId + ".json")
        .queryResult()
        .mapToOptional(WikidataEntityDataHash.class)
        .map(hash -> hash.getWikidataEntity(wikidataId));
  }
}
