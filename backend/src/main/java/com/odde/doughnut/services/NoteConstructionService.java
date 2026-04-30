package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteCreationResult;
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

  public Note createNote(Note parentNote, String title) {
    final Note note = new Note();
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    note.initialize(user, parentNote, currentUTCTimestamp, title);
    if (parentNote != null) {
      note.setFolder(noteChildContainerFolderService.resolveForParent(parentNote));
    }
    wikiSlugPathService.assignSlugForNewNote(note);
    if (entityPersister != null) {
      entityPersister.save(note);
    }
    return note;
  }

  private Note createRootNote(Notebook notebook, String title) {
    Note note = new Note();
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    note.initializeAsNotebookRoot(notebook, user, currentUTCTimestamp, title);
    wikiSlugPathService.assignSlugForNewNote(note);
    if (entityPersister != null) {
      entityPersister.save(note);
    }
    return note;
  }

  public Note createNoteUnderParentId(Integer parentNoteId, String title)
      throws UnexpectedNoAccessRightException {
    Note parentNote =
        noteRepository
            .findById(parentNoteId)
            .orElseThrow(() -> new RuntimeException("Parent note not found"));
    authorizationService.assertAuthorization(parentNote);
    return createNote(parentNote, title);
  }

  private Note createNoteWithWikidataInfo(
      Note parentNote, WikidataIdWithApi wikidataIdWithApi, String title)
      throws DuplicateWikidataIdException, IOException, InterruptedException {
    Note note = createNote(parentNote, title);
    return attachWikidataAndRefresh(note, wikidataIdWithApi);
  }

  private Note createRootNoteWithWikidataInfo(
      Notebook notebook, WikidataIdWithApi wikidataIdWithApi, String title)
      throws DuplicateWikidataIdException, IOException, InterruptedException {
    Note note = createRootNote(notebook, title);
    return attachWikidataAndRefresh(note, wikidataIdWithApi);
  }

  private Note attachWikidataAndRefresh(Note note, WikidataIdWithApi wikidataIdWithApi)
      throws DuplicateWikidataIdException, IOException, InterruptedException {
    if (wikidataIdWithApi != null) {
      wikidataIdWithApi.associateNoteToWikidata(note, noteService);
      wikidataIdWithApi.getCountryOfOrigin().ifPresent(wwa -> createSubNote(note, wwa));
      wikidataIdWithApi.getAuthors().forEach(wwa -> createSubNote(note, wwa));
    }
    entityPersister.flush();
    entityPersister.refresh(note);
    return note;
  }

  @SneakyThrows
  private void createSubNote(Note parentNote, WikidataIdWithApi subWikidataIdWithApi) {
    Optional<String> optionalTitle = subWikidataIdWithApi.fetchEnglishTitleFromApi();
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    optionalTitle.ifPresent(
        subNoteTitle ->
            noteRepository
                .noteWithWikidataIdWithinNotebook(
                    parentNote.getNotebook().getId(), subWikidataIdWithApi.wikidataId())
                .stream()
                .findFirst()
                .ifPresentOrElse(
                    existingNote -> {
                      noteService.createRelationship(
                          parentNote,
                          existingNote,
                          user,
                          RelationType.RELATED_TO,
                          currentUTCTimestamp);
                    },
                    () -> {
                      try {
                        createNoteWithWikidataInfo(parentNote, subWikidataIdWithApi, subNoteTitle);
                      } catch (Exception | DuplicateWikidataIdException e) {
                        throw new RuntimeException(e);
                      }
                    }));
  }

  public NoteCreationResult createNoteWithWikidataService(
      Note parentNote, NoteCreationDTO noteCreation, User user, WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException, BindException {
    try {
      Note note =
          createNoteWithWikidataInfo(parentNote, wikidataIdWithApi, noteCreation.getNewTitle());
      return new NoteCreationResult(
          noteRealmService.build(note, user), noteRealmService.build(parentNote, user));
    } catch (DuplicateWikidataIdException e) {
      BindingResult bindingResult = new BeanPropertyBindingResult(noteCreation, "noteCreation");
      bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
      throw new BindException(bindingResult);
    }
  }

  public NoteCreationResult createRootNoteWithWikidataService(
      Notebook notebook,
      NoteCreationDTO noteCreation,
      User user,
      WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException, BindException {
    try {
      Note note =
          createRootNoteWithWikidataInfo(notebook, wikidataIdWithApi, noteCreation.getNewTitle());
      return new NoteCreationResult(noteRealmService.build(note, user), null);
    } catch (DuplicateWikidataIdException e) {
      BindingResult bindingResult = new BeanPropertyBindingResult(noteCreation, "noteCreation");
      bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
      throw new BindException(bindingResult);
    }
  }

  public Note createNoteAfter(
      Note referenceNote,
      NoteCreationDTO noteCreation,
      User user,
      WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException, BindException {
    Note note =
        createNoteWithWikidataService(
                referenceNote.getParent(), noteCreation, user, wikidataIdWithApi)
            .getCreated()
            .getNote();
    note.setSiblingOrderToInsertAfter(referenceNote);
    note.adjustPositionAsAChildOfParentInMemory();
    entityPersister.save(note);
    return note;
  }

  public NoteCreationResult createNoteFromPromotedPointToChild(
      Note originalNote, PointExtractionResult aiResult) throws UnexpectedNoAccessRightException {
    return createNoteFromPromotedPoint(originalNote, originalNote.getId(), aiResult);
  }

  public NoteCreationResult createNoteFromPromotedPointToSibling(
      Note originalNote, PointExtractionResult aiResult) throws UnexpectedNoAccessRightException {
    return createNoteFromPromotedPoint(originalNote, originalNote.getParent().getId(), aiResult);
  }

  private NoteCreationResult createNoteFromPromotedPoint(
      Note originalNote, Integer parentNoteId, PointExtractionResult aiResult)
      throws UnexpectedNoAccessRightException {
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    Note newNote = createNoteUnderParentId(parentNoteId, aiResult.newNoteTitle);
    newNote.setDetails(aiResult.newNoteDetails);
    newNote.setUpdatedAt(currentUTCTimestamp);
    entityPersister.save(newNote);

    originalNote.setUpdatedAt(currentUTCTimestamp);
    originalNote.setDetails(aiResult.updatedParentDetails);
    entityPersister.save(originalNote);

    return new NoteCreationResult(
        noteRealmService.build(newNote, user), noteRealmService.build(originalNote, user));
  }
}
