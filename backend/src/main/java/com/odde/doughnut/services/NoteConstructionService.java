package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteCreationRresult;
import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.DuplicateWikidataIdException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Optional;
import lombok.SneakyThrows;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;

public record NoteConstructionService(
    User user, Timestamp currentUTCTimestamp, ModelFactoryService modelFactoryService) {

  public Note createNote(Note parentNote, String topicConstructor) {
    final Note note = new Note();
    note.initialize(user, parentNote, currentUTCTimestamp, topicConstructor);
    if (modelFactoryService != null) {
      modelFactoryService.save(note);
    }
    return note;
  }

  private Note createNoteWithWikidataInfo(
      Note parentNote,
      WikidataIdWithApi wikidataIdWithApi,
      LinkType linkTypeToParent,
      String topicConstructor)
      throws DuplicateWikidataIdException, IOException, InterruptedException {
    Note note = createNote(parentNote, topicConstructor);
    modelFactoryService.createLink(
        note, note.getParent(), user, linkTypeToParent, currentUTCTimestamp);
    if (wikidataIdWithApi != null) {
      wikidataIdWithApi.associateNoteToWikidata(note, modelFactoryService);
      wikidataIdWithApi.getCountryOfOrigin().ifPresent(wwa -> createSubNote(note, wwa));
      wikidataIdWithApi.getAuthors().forEach(wwa -> createSubNote(note, wwa));
    }
    modelFactoryService.entityManager.flush();
    modelFactoryService.entityManager.refresh(note);
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
                      modelFactoryService.createLink(
                          parentNote, existingNote, user, LinkType.RELATED_TO, currentUTCTimestamp);
                    },
                    () -> {
                      try {
                        createNoteWithWikidataInfo(
                            parentNote, subWikidataIdWithApi, LinkType.RELATED_TO, subNoteTitle);
                      } catch (Exception | DuplicateWikidataIdException e) {
                        throw new RuntimeException(e);
                      }
                    }));
  }

  public NoteCreationRresult createNoteWithWikidataService(
      Note parentNote, NoteCreationDTO noteCreation, User user, WikidataService wikidataService1)
      throws InterruptedException, IOException, BindException {
    try {
      Note note =
          createNoteWithWikidataInfo(
              parentNote,
              wikidataService1.wrapWikidataIdWithApi(noteCreation.wikidataId),
              noteCreation.getLinkTypeToParent(),
              noteCreation.getTopicConstructor());
      return new NoteCreationRresult(
          new NoteViewer(user, note).toJsonObject(),
          new NoteViewer(user, parentNote).toJsonObject());
    } catch (DuplicateWikidataIdException e) {
      BindingResult bindingResult = new BeanPropertyBindingResult(noteCreation, "noteCreation");
      bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
      throw new BindException(bindingResult);
    }
  }

  public Note createNoteAfter(
      Note referenceNote,
      NoteCreationDTO noteCreation,
      Note parentNote,
      User user,
      WikidataService wikidataService1)
      throws InterruptedException, IOException, BindException {
    Note note =
        createNoteWithWikidataService(parentNote, noteCreation, user, wikidataService1)
            .getCreated()
            .getNote();
    note.setSiblingOrderToInsertAfter(referenceNote);
    note.adjustPositionAsAChildOfParentInMemory();
    modelFactoryService.save(note);
    return note;
  }
}
