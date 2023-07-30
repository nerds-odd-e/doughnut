package com.odde.doughnut.services.wikidataApis;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataEntityHash;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;
import org.springframework.validation.BindException;

public final class WikidataIdWithApi {
  private final String wikidataId;
  private final WikidataApi wikidataApi;

  public static WikidataIdWithApi create(String wikidataId, WikidataApi wikidataApi) {
    if (Strings.isBlank(wikidataId)) return null;
    return new WikidataIdWithApi(wikidataId, wikidataApi);
  }

  private WikidataIdWithApi(String wikidataId, WikidataApi wikidataApi) {
    this.wikidataId = wikidataId;
    this.wikidataApi = wikidataApi;
  }

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
  }

  public Optional<WikidataIdWithApi> getCountryOfOrigin() throws IOException, InterruptedException {
    Optional<WikidataEntityModel> model = getWikidataEntityModel();
    return model.flatMap(entity -> entity.getCountryOfOrigin(wikidataApi));
  }

  public Stream<WikidataIdWithApi> getAuthors() throws IOException, InterruptedException {
    Optional<WikidataEntityModel> model = getWikidataEntityModel();
    return model.map(i -> i.getAuthorList(wikidataApi)).orElse(Stream.empty());
  }

  public void associateNoteToWikidata(Note note, ModelFactoryService modelFactoryService)
      throws BindException, IOException, InterruptedException {
    note.setWikidataId(this.wikidataId);
    modelFactoryService.toNoteModel(note).checkDuplicateWikidataId();
    extractWikidataInfoToNote(note);
  }

  public String wikidataId() {
    return wikidataId;
  }

  public WikidataApi wikidataApi() {
    return wikidataApi;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (WikidataIdWithApi) obj;
    return Objects.equals(this.wikidataId, that.wikidataId)
        && Objects.equals(this.wikidataApi, that.wikidataApi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(wikidataId, wikidataApi);
  }

  @Override
  public String toString() {
    return "WikidataIdWithApi["
        + "wikidataId="
        + wikidataId
        + ", "
        + "wikidataApi="
        + wikidataApi
        + ']';
  }
}
