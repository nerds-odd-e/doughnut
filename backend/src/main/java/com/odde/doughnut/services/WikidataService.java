package com.odde.doughnut.services;

import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.services.externalApis.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    return getWikidataEntityModel(wikidataId).map(this::wikidataDescription);
  }

  private String wikidataDescription(WikidataEntityModelOfProperties entity) {
    if (entity.getInstanceOf().map(WikidataId::isHuman).orElse(false)) {
      return Stream.of(
              entity
                  .getCountryOfOriginValue()
                  .flatMap(wikidataId1 -> wikidataId1.fetchEnglishTitleFromApi(wikidataApi)),
              entity.getBirthdayData().map(WikidataDate::format))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .filter(value -> !value.isBlank())
          .collect(Collectors.joining(", "));
    }
    return entity.getGeographicCoordinate().map(Coordinate::toLocationDescription).orElse(null);
  }

  @SneakyThrows
  public Optional<Coordinate> fetchWikidataCoordinate(String wikidataId) {
    return getWikidataEntityModel(wikidataId)
        .flatMap(WikidataEntityModelOfProperties::getCoordinate);
  }

  private Optional<WikidataEntityModelOfProperties> getWikidataEntityModel(String wikidataId)
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
