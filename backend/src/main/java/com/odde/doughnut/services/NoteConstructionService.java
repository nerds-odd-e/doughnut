package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.DuplicateWikidataIdException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.PointExtractionResult;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteConstructionService {
  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;
  private final NoteRepository noteRepository;
  private final FolderRepository folderRepository;
  private final EntityPersister entityPersister;
  private final NoteService noteService;
  private final NoteChildContainerFolderService noteChildContainerFolderService;
  private final NoteRealmService noteRealmService;

  @Autowired
  public NoteConstructionService(
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings,
      NoteRepository noteRepository,
      FolderRepository folderRepository,
      EntityPersister entityPersister,
      NoteService noteService,
      NoteChildContainerFolderService noteChildContainerFolderService,
      NoteRealmService noteRealmService) {
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.noteRepository = noteRepository;
    this.folderRepository = folderRepository;
    this.entityPersister = entityPersister;
    this.noteService = noteService;
    this.noteChildContainerFolderService = noteChildContainerFolderService;
    this.noteRealmService = noteRealmService;
  }

  public Note createNote(Notebook notebook, Note parentNote, String title) {
    Objects.requireNonNull(notebook, "notebook");
    if (parentNote != null) {
      Folder folder = noteChildContainerFolderService.resolveForParent(parentNote);
      return persistNewNoteInNotebookFolder(notebook, folder, title);
    }
    return createNoteInNotebookScopeWithoutWikidata(notebook, null, title);
  }

  private Note createNoteInNotebookScopeWithoutWikidata(
      Notebook notebook, Folder folderOrNull, String title) {
    if (folderOrNull != null) {
      return persistNewNoteInNotebookFolder(notebook, folderOrNull, title);
    }
    Note note = new Note();
    User user = authorizationService.getCurrentUser();
    Timestamp ts = testabilitySettings.getCurrentUTCTimestamp();
    note.initializeAsNotebookRoot(notebook, user, ts, title);
    assignSiblingOrderAppendLast(note);
    if (entityPersister != null) {
      entityPersister.save(note);
    }
    return note;
  }

  private void assignSiblingOrderAppendLast(Note note) {
    List<Note> peers;
    if (note.getFolder() != null) {
      peers = noteRepository.findNotesInFolderOrderBySiblingOrder(note.getFolder().getId());
    } else {
      peers =
          noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(note.getNotebook().getId());
    }
    long next =
        peers.isEmpty()
            ? SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT
            : peers.get(peers.size() - 1).getSiblingOrder()
                + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
    note.setSiblingOrder(next);
  }

  private Note persistNewNoteInNotebookFolder(Notebook notebook, Folder folder, String title) {
    Objects.requireNonNull(notebook, "notebook");
    Objects.requireNonNull(folder, "folder");
    Note note = new Note();
    User user = authorizationService.getCurrentUser();
    Timestamp ts = testabilitySettings.getCurrentUTCTimestamp();
    note.initializeNewNote(user, notebook, ts, title);
    note.assignNotebook(notebook);
    note.setFolder(folder);
    assignSiblingOrderAppendLast(note);
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
                        if (focalNote.getFolder() != null) {
                          attachWikidataAndRefresh(
                              persistNewNoteInNotebookFolder(
                                  focalNote.getNotebook(), focalNote.getFolder(), siblingTitle),
                              subWikidataIdWithApi);
                        } else {
                          createNoteWithWikidataInfo(
                              focalNote.getNotebook(), null, subWikidataIdWithApi, siblingTitle);
                        }
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
    Folder folder = null;
    if (noteCreation.getFolderId() != null) {
      folder =
          folderRepository
              .findById(noteCreation.getFolderId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found."));
      if (!folder.getNotebook().getId().equals(notebook.getId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder not in notebook.");
      }
    }
    try {
      Note note =
          createNoteInNotebookScopeWithoutWikidata(notebook, folder, noteCreation.getNewTitle());
      note = attachWikidataAndRefresh(note, wikidataIdWithApi);
      return noteRealmService.build(note, user);
    } catch (DuplicateWikidataIdException e) {
      throw duplicateWikidataBinding(noteCreation);
    }
  }

  public NoteRealm createNoteFromPromotedPointToSibling(
      Note originalNote, PointExtractionResult aiResult) throws UnexpectedNoAccessRightException {
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    Note newNote =
        createNoteInNotebookScopeWithoutWikidata(
            originalNote.getNotebook(), originalNote.getFolder(), aiResult.newNoteTitle);
    newNote.setDetails(aiResult.newNoteDetails);
    newNote.setUpdatedAt(currentUTCTimestamp);
    entityPersister.save(newNote);

    originalNote.setUpdatedAt(currentUTCTimestamp);
    originalNote.setDetails(aiResult.updatedParentDetails);
    entityPersister.save(originalNote);

    return noteRealmService.build(newNote, user);
  }
}
