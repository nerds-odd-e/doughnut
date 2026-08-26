package com.odde.donut.services.wikidataApis;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.odde.donut.controllers.dto.WikidataEntityData;
import com.odde.donut.exceptions.WikidataServiceErrorException;
import com.odde.donut.services.httpQuery.QueryBuilder;
import com.odde.donut.services.httpQuery.QueryResult;
import com.odde.donut.services.wikidataApis.thirdPartyEntities.WikidataEntityDataHash;
import com.odde.donut.services.wikidataApis.thirdPartyEntities.WikidataEntityHash;
import com.odde.donut.services.wikidataApis.thirdPartyEntities.WikidataSearchResult;
import java.io.IOException;
import java.util.Optional;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;

public record WikidataApi(QueryBuilder queryBuilder) {

  private QueryBuilder queryWikidataApi(String action) {
    return queryBuilder.path("/w/api.php").queryParam("action", action);
  }

  public WikidataSearchResult getWikidataSearchEntities(String search)
      throws IOException, InterruptedException {
    QueryResult queryResult =
        queryWikidataApi("wbsearchentities")
            .queryParam("search", "{search}")
            .queryParam("format", "json")
            .queryParam("language", "en")
            .queryParam("uselang", "en")
            .queryParam("type", "item")
            .queryParam("limit", 10)
            .queryResult(search);
    WikidataSearchResult result = queryResult.mapToObject(WikidataSearchResult.class);
    if (result == null || result.search == null) {
      throw new WikidataServiceErrorException(
          "Wikidata search response missing 'search' list. Raw result: " + queryResult.response(),
          HttpStatus.BAD_GATEWAY);
    }
    return result;
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
