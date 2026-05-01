package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.DuplicateWikidataIdException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.PointExtractionResult;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;

@Service
public class NoteConstructionService {
  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;
  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;
  private final NoteService noteService;
  private final NoteChildContainerFolderService noteChildContainerFolderService;
  private final WikiSlugPathService wikiSlugPathService;
  private final NoteRealmService noteRealmService;

  @Autowired
  public NoteConstructionService(
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings,
      NoteRepository noteRepository,
      EntityPersister entityPersister,
      NoteService noteService,
      NoteChildContainerFolderService noteChildContainerFolderService,
      WikiSlugPathService wikiSlugPathService,
      NoteRealmService noteRealmService) {
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
    this.noteService = noteService;
    this.noteChildContainerFolderService = noteChildContainerFolderService;
    this.wikiSlugPathService = wikiSlugPathService;
    this.noteRealmService = noteRealmService;
  }

  public Note createNote(Notebook notebook, Note parentNote, String title) {
    Objects.requireNonNull(notebook, "notebook");
    final Note note = new Note();
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    if (parentNote != null) {
      note.initialize(user, parentNote, currentUTCTimestamp, title);
      note.setFolder(noteChildContainerFolderService.resolveForParent(parentNote));
    } else {
      note.initializeAsNotebookRoot(notebook, user, currentUTCTimestamp, title);
    }
    wikiSlugPathService.assignSlugForNewNote(note);
    if (entityPersister != null) {
      entityPersister.save(note);
    }
    return note;
  }

  private Note createNoteWithWikidataInfo(
      Notebook notebook, Note parentNote, WikidataIdWithApi wikidataIdWithApi, String title)
      throws DuplicateWikidataIdException, IOException, InterruptedException {
    Note note = createNote(notebook, parentNote, title);
    return attachWikidataAndRefresh(note, wikidataIdWithApi);
  }

  private Note attachWikidataAndRefresh(Note note, WikidataIdWithApi wikidataIdWithApi)
      throws DuplicateWikidataIdException, IOException, InterruptedException {
    if (wikidataIdWithApi != null) {
      wikidataIdWithApi.associateNoteToWikidata(note, noteService);
      wikidataIdWithApi
          .getCountryOfOrigin()
          .ifPresent(wwa -> addWikidataLinkedSiblingNote(note, wwa));
      wikidataIdWithApi.getAuthors().forEach(wwa -> addWikidataLinkedSiblingNote(note, wwa));
    }
    entityPersister.flush();
    entityPersister.refresh(note);
    return note;
  }

  @SneakyThrows
  private void addWikidataLinkedSiblingNote(
      Note focalNote, WikidataIdWithApi subWikidataIdWithApi) {
    Optional<String> optionalTitle = subWikidataIdWithApi.fetchEnglishTitleFromApi();
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    optionalTitle.ifPresent(
        siblingTitle ->
            noteRepository
                .noteWithWikidataIdWithinNotebook(
                    focalNote.getNotebook().getId(), subWikidataIdWithApi.wikidataId())
                .stream()
                .findFirst()
                .ifPresentOrElse(
                    existingNote -> {
                      noteService.createRelationship(
                          focalNote,
                          existingNote,
                          user,
                          RelationType.RELATED_TO,
                          currentUTCTimestamp);
                    },
                    () -> {
                      try {
                        Note siblingParentNote = focalNote.getParent();
                        createNoteWithWikidataInfo(
                            focalNote.getNotebook(),
                            siblingParentNote,
                            subWikidataIdWithApi,
                            siblingTitle);
                      } catch (Exception | DuplicateWikidataIdException e) {
                        throw new RuntimeException(e);
                      }
                    }));
  }

  private BindException duplicateWikidataBinding(NoteCreationDTO noteCreation) {
    BindingResult bindingResult = new BeanPropertyBindingResult(noteCreation, "noteCreation");
    bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
    return new BindException(bindingResult);
  }

  public NoteRealm createNoteWithWikidataService(
      Note parentNote, NoteCreationDTO noteCreation, User user, WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException, BindException {
    try {
      Note note =
          createNoteWithWikidataInfo(
              parentNote.getNotebook(), parentNote, wikidataIdWithApi, noteCreation.getNewTitle());
      return noteRealmService.build(note, user);
    } catch (DuplicateWikidataIdException e) {
      throw duplicateWikidataBinding(noteCreation);
    }
  }

  public NoteRealm createRootNoteWithWikidataService(
      Notebook notebook,
      NoteCreationDTO noteCreation,
      User user,
      WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException, BindException {
    try {
      Note note =
          createNoteWithWikidataInfo(notebook, null, wikidataIdWithApi, noteCreation.getNewTitle());
      return noteRealmService.build(note, user);
    } catch (DuplicateWikidataIdException e) {
      throw duplicateWikidataBinding(noteCreation);
    }
  }

  public Note createNoteAfter(
      Note referenceNote, NoteCreationDTO noteCreation, WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException, BindException {
    try {
      Note note =
          createNoteWithWikidataInfo(
              referenceNote.getNotebook(),
              referenceNote.getParent(),
              wikidataIdWithApi,
              noteCreation.getNewTitle());
      note.setSiblingOrderToInsertAfter(referenceNote);
      note.adjustPositionAsAChildOfParentInMemory();
      entityPersister.save(note);
      return note;
    } catch (DuplicateWikidataIdException e) {
      throw duplicateWikidataBinding(noteCreation);
    }
  }

  public NoteRealm createNoteFromPromotedPointToSibling(
      Note originalNote, PointExtractionResult aiResult) throws UnexpectedNoAccessRightException {
    return createNoteFromPromotedPoint(originalNote, originalNote.getParent(), aiResult);
  }

  private NoteRealm createNoteFromPromotedPoint(
      Note originalNote, Note parentNote, PointExtractionResult aiResult)
      throws UnexpectedNoAccessRightException {
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    Note newNote = createNote(originalNote.getNotebook(), parentNote, aiResult.newNoteTitle);
    newNote.setDetails(aiResult.newNoteDetails);
    newNote.setUpdatedAt(currentUTCTimestamp);
    entityPersister.save(newNote);

    originalNote.setUpdatedAt(currentUTCTimestamp);
    originalNote.setDetails(aiResult.updatedParentDetails);
    entityPersister.save(originalNote);

    return noteRealmService.build(newNote, user);
  }
}
