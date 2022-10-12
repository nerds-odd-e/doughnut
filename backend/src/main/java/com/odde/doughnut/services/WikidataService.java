package com.odde.doughnut.services;

import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.services.wikidataApis.*;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataEntityHash;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataService(WikidataApi wikidataApi) {
  public WikidataService(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
    this(
        new WikidataApi(
            new QueryBuilder(httpClientAdapter, UriComponentsBuilder.fromHttpUrl(wikidataUrl))));
  }

  public Optional<WikidataEntityData> fetchWikidataEntityData(String wikidataId)
      throws IOException {
    return wikidataApi.getWikidataEntityData(wikidataId);
  }

  public List<WikidataSearchEntity> searchWikidata(String search)
      throws IOException, InterruptedException {
    return wikidataApi.getWikidataSearchEntities(search).getWikidataSearchEntities();
  }

  @SneakyThrows
  private Optional<String> fetchWikidataDescription(String wikidataId) {
    return getWikidataEntityModel(wikidataId)
        .map(entity -> entity.wikidataDescription(wikidataApi));
  }

  @SneakyThrows
  private Optional<Coordinate> fetchWikidataCoordinate(String wikidataId) {
    return getWikidataEntityModel(wikidataId).flatMap(WikidataEntityModel::getCoordinate);
  }

  private Optional<WikidataEntityModel> getWikidataEntityModel(String wikidataId)
      throws IOException, InterruptedException {
    WikidataEntityHash entityHash = wikidataApi.getEntityHashById(wikidataId);
    if (entityHash == null) return Optional.empty();
    return entityHash.getEntityModel(wikidataId);
  }

  public void extractWikidataInfoToNote(String wikidataId, Note note) {
    fetchWikidataDescription(wikidataId).ifPresent(note::prependDescription);
    fetchWikidataCoordinate(wikidataId).ifPresent(note::buildLocation);
  }
}
