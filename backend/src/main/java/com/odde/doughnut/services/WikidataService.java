package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.NoteCreation;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.wikidataApis.*;
import java.io.IOException;
import java.util.List;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataService(WikidataApi wikidataApi) {
  public WikidataService(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
    this(
        new WikidataApi(
            new QueryBuilder(httpClientAdapter, UriComponentsBuilder.fromHttpUrl(wikidataUrl))));
  }

  public List<WikidataSearchEntity> searchWikidata(String search)
      throws IOException, InterruptedException {
    return wikidataApi.getWikidataSearchEntities(search).getWikidataSearchEntities();
  }

  public WikidataIdWithApi wrapWikidataIdWithApi(String wikidataId) {
    return new WikidataId(wikidataId).withApi(wikidataApi);
  }

  @SneakyThrows
  public WikidataIdWithApi associateToWikidata(
      Note note, String wikidataId, ModelFactoryService modelFactoryService) {
    note.setWikidataId(wikidataId);
    modelFactoryService.toNoteModel(note).checkDuplicateWikidataId();
    return wrapWikidataIdWithApi(wikidataId);
  }

  @NotNull
  public WikidataIdWithApi associateToWikiDataAndExtractInfoToNote(
      NoteCreation noteCreation, Note note, ModelFactoryService modelFactoryService)
      throws IOException, InterruptedException {
    WikidataIdWithApi wikidataIdWithApi =
        associateToWikidata(note, noteCreation.wikidataId, modelFactoryService);
    wikidataIdWithApi.extractWikidataInfoToNote(note);
    return wikidataIdWithApi;
  }
}
