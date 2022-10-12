package com.odde.doughnut.services;

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

  private Optional<WikidataEntityModel> getWikidataEntityModel(String wikidataId)
      throws IOException, InterruptedException {
    WikidataEntityHash entityHash = wikidataApi.getEntityHashById(wikidataId);
    if (entityHash == null) return Optional.empty();
    return entityHash.getEntityModel(wikidataId);
  }

  @SneakyThrows
  public void extractWikidataInfoToNote(String wikidataId, Note note) {
    Optional<WikidataEntityModel> wikidataEntityModel = getWikidataEntityModel(wikidataId);
    wikidataEntityModel
        .map(entity -> entity.wikidataDescription(wikidataApi))
        .ifPresent(note::prependDescription);
    wikidataEntityModel.flatMap(WikidataEntityModel::getCoordinate).ifPresent(note::buildLocation);
  }
}
