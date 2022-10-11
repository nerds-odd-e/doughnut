package com.odde.doughnut.services;

import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.services.externalApis.*;
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

  public List<WikidataSearchEntity> fetchWikidataByQuery(String search)
      throws IOException, InterruptedException {
    return wikidataApi.getWikidataSearchEntities(search).getWikidataSearchEntities();
  }

  @SneakyThrows
  public Optional<String> fetchWikidataDescription(String wikidataId) {
    return getWikidataEntity(wikidataId)
        .flatMap(
            x -> {
              if (x.getInstanceOf().map(WikidataId::isHuman).orElse(false)) {
                return Optional.of(x.getHumanDescription(wikidataApi));
              }
              return x.getCountryDescription();
            });
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

  public void extractWikidataInfoToNote(String wikidataId, Note note) {
    fetchWikidataDescription(wikidataId).ifPresent(note::prependDescription);
    fetchWikidataCoordinate(wikidataId).ifPresent(note::buildLocation);
  }
}
