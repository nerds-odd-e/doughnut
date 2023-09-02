package com.odde.doughnut.services;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.TextContent;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import java.sql.Timestamp;
import java.util.Optional;
import lombok.SneakyThrows;

public record NoteConstructionService(
    User user, Timestamp currentUTCTimestamp, ModelFactoryService modelFactoryService) {

  @SneakyThrows
  public Note createNoteWithWikidataInfo(
      Note parentNote,
      WikidataIdWithApi wikidataIdWithApi,
      TextContent textContent,
      Link.LinkType linkTypeToParent) {
    Note note = parentNote.buildChildNote(user, currentUTCTimestamp, textContent);
    note.buildLinkToParent(user, linkTypeToParent, currentUTCTimestamp);
    modelFactoryService.noteRepository.save(note);
    if (wikidataIdWithApi != null) {
      wikidataIdWithApi.associateNoteToWikidata(note, modelFactoryService);
      wikidataIdWithApi.getCountryOfOrigin().ifPresent(wwa -> createSubNote(note, wwa));
      wikidataIdWithApi.getAuthors().forEach(wwa -> createSubNote(note, wwa));
    }
    return note;
  }

  @SneakyThrows
  private void createSubNote(Note parentNote, WikidataIdWithApi subWikidataIdWithApi) {
    Optional<String> optionalTitle = subWikidataIdWithApi.fetchEnglishTitleFromApi();
    optionalTitle.ifPresent(
        subNoteTitle ->
            modelFactoryService
                .noteRepository
                .noteWithWikidataIdWithinNotebook(
                    parentNote.getNotebook(), subWikidataIdWithApi.wikidataId())
                .stream()
                .findFirst()
                .ifPresentOrElse(
                    existingNote -> {
                      Link link =
                          parentNote.buildLinkToNote(
                              user, Link.LinkType.RELATED_TO, currentUTCTimestamp, existingNote);
                      this.modelFactoryService.linkRepository.save(link);
                    },
                    () -> {
                      TextContent textContent = new TextContent();
                      textContent.setTopic(subNoteTitle);
                      createNoteWithWikidataInfo(
                          parentNote, subWikidataIdWithApi, textContent, Link.LinkType.RELATED_TO);
                    }));
  }
}
