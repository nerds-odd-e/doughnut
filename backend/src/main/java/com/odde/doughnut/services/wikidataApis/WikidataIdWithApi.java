package com.odde.doughnut.services.wikidataApis;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataEntityHash;
import java.io.IOException;
import java.util.Optional;

public record WikidataIdWithApi(String wikidataId, WikidataApi wikidataApi) {
  public Optional<String> fetchEnglishTitleFromApi() {
    return wikidataApi.getWikidataEntityData(wikidataId()).map(e -> e.WikidataTitleInEnglish);
  }

  public Optional<WikidataEntityData> fetchWikidataEntityData() throws IOException {
    return wikidataApi.getWikidataEntityData(wikidataId);
  }

  private Optional<WikidataEntityModel> getWikidataEntityModel()
      throws IOException, InterruptedException {
    WikidataEntityHash entityHash = wikidataApi.getEntityHashById(wikidataId);
    if (entityHash == null) return Optional.empty();
    return entityHash.getEntityModel(wikidataId);
  }

  public void extractWikidataInfoToNote(Note note) throws IOException, InterruptedException {
    Optional<WikidataEntityModel> model = getWikidataEntityModel();
    model
        .map(entity -> entity.wikidataDescription(wikidataApi))
        .ifPresent(note::prependDescription);
    model.flatMap(WikidataEntityModel::getCoordinate).ifPresent(note::buildLocation);
  }

  public Optional<WikidataIdWithApi> getCountryOfOrigin() throws IOException, InterruptedException {
    Optional<WikidataEntityModel> model = getWikidataEntityModel();
    return model.flatMap(entity -> entity.getCountryOfOrigin(wikidataApi));
  }

  public Optional<WikidataIdWithApi> getAuthor() throws IOException, InterruptedException {
    Optional<WikidataEntityModel> model = getWikidataEntityModel();
    return model.flatMap(entity -> entity.getAuthor(wikidataApi));
  }
}
