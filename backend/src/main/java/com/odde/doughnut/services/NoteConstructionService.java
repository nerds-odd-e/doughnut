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
    wikidataIdWithApi.associateNoteToWikidata(note, modelFactoryService);
    note.buildLinkToParent(user, linkTypeToParent, currentUTCTimestamp);
    modelFactoryService.noteRepository.save(note);

    wikidataIdWithApi.getCountryOfOrigin().ifPresent(wwa -> createSubNote(note, wwa));
    wikidataIdWithApi.getAuthor().ifPresent(wwa -> createSubNote(note, wwa));

    return note;
  }

  @SneakyThrows
  private void createSubNote(Note parentNote, WikidataIdWithApi subWikidataIdWithApi) {
    Optional<String> optionalTitle = subWikidataIdWithApi.fetchEnglishTitleFromApi();
    optionalTitle.ifPresent(
        subNoteTitle -> {
          Optional<Note> existingNoteOption =
              parentNote
                  .getNotebook()
                  .findExistingNoteInNotebook(subWikidataIdWithApi.wikidataId());
          if (existingNoteOption.isPresent()) {
            Link link =
                parentNote.buildLinkToNote(
                    user, Link.LinkType.RELATED_TO, currentUTCTimestamp, existingNoteOption.get());
            this.modelFactoryService.linkRepository.save(link);
          } else {
            TextContent textContent = new TextContent();
            textContent.setTitle(subNoteTitle);
            createNoteWithWikidataInfo(
                parentNote, subWikidataIdWithApi, textContent, Link.LinkType.RELATED_TO);
          }
        });
  }
}
