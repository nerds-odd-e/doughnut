package com.odde.doughnut.services;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.TextContent;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.DuplicateWikidataIdException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Optional;
import lombok.SneakyThrows;

public record NoteConstructionService(
    User user, Timestamp currentUTCTimestamp, ModelFactoryService modelFactoryService) {

  public Note createNoteWithWikidataInfo(
      Note parentNote,
      WikidataIdWithApi wikidataIdWithApi,
      TextContent textContent,
      Link.LinkType linkTypeToParent)
      throws DuplicateWikidataIdException, IOException, InterruptedException {
    Note note = parentNote.buildChildNote(user, currentUTCTimestamp, textContent);
    note.buildLinkToParent(user, linkTypeToParent, currentUTCTimestamp);
    modelFactoryService.save(note);
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
                    parentNote.getNotebook().getId(), subWikidataIdWithApi.wikidataId())
                .stream()
                .findFirst()
                .ifPresentOrElse(
                    existingNote -> {
                      Link link =
                          parentNote.buildLinkToNote(
                              user, Link.LinkType.RELATED_TO, currentUTCTimestamp, existingNote);
                      this.modelFactoryService.save(link);
                    },
                    () -> {
                      TextContent textContent = new TextContent();
                      textContent.setTopic(subNoteTitle);
                      try {
                        createNoteWithWikidataInfo(
                            parentNote,
                            subWikidataIdWithApi,
                            textContent,
                            Link.LinkType.RELATED_TO);
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      } catch (DuplicateWikidataIdException e) {
                        throw new RuntimeException(e);
                      }
                    }));
  }
}
