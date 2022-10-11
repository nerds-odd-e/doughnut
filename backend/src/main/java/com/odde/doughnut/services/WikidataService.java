package com.odde.doughnut.services;

import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.services.externalApis.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;

public record WikidataService(WikidataApi wikidataApi) {
  public WikidataService(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
    this(new WikidataApi(httpClientAdapter, wikidataUrl));
  }

  public Optional<WikidataEntityData> fetchWikidataEntityData(String wikidataId)
      throws IOException, InterruptedException {
    return wikidataApi.getWikidataEntityData(wikidataId);
  }

  public List<WikidataSearchEntity> fetchWikidataByQuery(String search)
      throws IOException, InterruptedException {
    return wikidataApi.getWikidataSearchEntities(search).getWikidataSearchEntities();
  }

  @SneakyThrows
  public Optional<String> fetchWikidataDescription(String wikidataId) {
    return getWikidataEntity(wikidataId).flatMap(x -> x.getDescription(wikidataApi));
  }

  @SneakyThrows
  public Optional<Coordinate> fetchWikidataCoordinate(String wikidataId) {
    return getWikidataEntity(wikidataId).flatMap(WikidataEntity::getCoordinate);
  }

  private Optional<WikidataEntity> getWikidataEntity(String wikidataId)
      throws IOException, InterruptedException {
    WikidataEntityHash entityHash = wikidataApi.getEntityHashById(wikidataId);
    if (entityHash == null) return Optional.empty();
    return entityHash.getEntity(wikidataId);
  }
}
