package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.PointExtractionResult;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteConstructionService {

  private static final String RESERVED_INDEX_TITLE_MESSAGE =
      "'index' is reserved for notebook and folder index content.";
  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;
  private final NoteRepository noteRepository;
  private final FolderRepository folderRepository;
  private final EntityPersister entityPersister;
  private final NoteRealmService noteRealmService;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteService noteService;

  @Autowired
  public NoteConstructionService(
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings,
      NoteRepository noteRepository,
      FolderRepository folderRepository,
      EntityPersister entityPersister,
      NoteRealmService noteRealmService,
      WikiTitleCacheService wikiTitleCacheService,
      NoteService noteService) {
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.noteRepository = noteRepository;
    this.folderRepository = folderRepository;
    this.entityPersister = entityPersister;
    this.noteRealmService = noteRealmService;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.noteService = noteService;
  }

  private Note createNote(Notebook notebook, Folder folderOrNull, String title) {
    throwIfReservedTitle(title);
    throwIfSoftDeletedTitleBlocks(notebook, folderOrNull, title);
    if (folderOrNull != null) {
      return persistNewNoteInNotebookFolder(notebook, folderOrNull, title);
    }
    Note note = new Note();
    User user = authorizationService.getCurrentUser();
    Timestamp ts = testabilitySettings.getCurrentUTCTimestamp();
    note.initializeNewNote(user, notebook, ts, title);
    if (entityPersister != null) {
      entityPersister.save(note);
    }
    return note;
  }

  private Note persistNewNoteInNotebookFolder(Notebook notebook, Folder folder, String title) {
    Objects.requireNonNull(notebook, "notebook");
    Objects.requireNonNull(folder, "folder");
    throwIfSoftDeletedTitleBlocks(notebook, folder, title);
    Note note = new Note();
    User user = authorizationService.getCurrentUser();
    Timestamp ts = testabilitySettings.getCurrentUTCTimestamp();
    note.initializeNewNote(user, notebook, ts, title);
    note.assignNotebook(notebook);
    note.setFolder(folder);
    if (entityPersister != null) {
      entityPersister.save(note);
    }
    return note;
  }

  private Note attachWikidataAndRefresh(Note note, WikidataIdWithApi wikidataIdWithApi)
      throws IOException, InterruptedException {
    if (wikidataIdWithApi != null) {
      wikidataIdWithApi.associateNoteToWikidata(note);
    }
    entityPersister.flush();
    entityPersister.refresh(note);
    return note;
  }

  public NoteRealm createRootNoteWithWikidataService(
      Notebook notebook,
      NoteCreationDTO noteCreation,
      User user,
      WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException {
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
    Note note = createNote(notebook, folder, noteCreation.getNewTitle());
    if (noteCreation.getContent() != null) {
      Timestamp ts = testabilitySettings.getCurrentUTCTimestamp();
      note.setContent(noteCreation.getContent());
      note.setUpdatedAt(ts);
      entityPersister.save(note);
    }
    note = attachWikidataAndRefresh(note, wikidataIdWithApi);
    noteService.deleteOrphanImagesForPersistedContent(note);
    if (noteCreation.getContent() != null || wikidataIdWithApi != null) {
      wikiTitleCacheService.refreshForNote(note, user);
    }
    return noteRealmService.build(note, user);
  }

  private void throwIfReservedTitle(String title) {
    if (title != null && title.trim().equalsIgnoreCase("index")) {
      ApiError apiError =
          new ApiError(RESERVED_INDEX_TITLE_MESSAGE, ApiError.ErrorType.BINDING_ERROR);
      apiError.add("newTitle", RESERVED_INDEX_TITLE_MESSAGE);
      throw new ApiException(apiError);
    }
  }

  private void throwIfSoftDeletedTitleBlocks(Notebook notebook, Folder folderOrNull, String title) {
    String trimmed = title != null ? title.trim() : "";
    if (trimmed.isEmpty()) {
      return;
    }
    Integer folderId = folderOrNull != null ? folderOrNull.getId() : null;
    List<Note> matches =
        noteRepository.findSoftDeletedByNotebookFolderAndTitleOrderByIdAsc(
            notebook.getId(), folderId, trimmed, PageRequest.of(0, 1));
    if (matches.isEmpty()) {
      return;
    }
    Note deleted = matches.getFirst();
    ApiError apiError =
        new ApiError(
            "A note with this title already exists here but was deleted. Restore the deleted note"
                + " (Undo delete), or choose another title.",
            ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT);
    apiError.add("deletedNoteId", String.valueOf(deleted.getId()));
    throw new ApiException(apiError);
  }

  public NoteRealm createNoteFromPromotedPointToSibling(
      Note originalNote, PointExtractionResult aiResult) {
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    Note newNote =
        createNote(originalNote.getNotebook(), originalNote.getFolder(), aiResult.newNoteTitle);
    newNote.setContent(aiResult.newNoteContent);
    newNote.setUpdatedAt(currentUTCTimestamp);
    entityPersister.save(newNote);

    originalNote.setUpdatedAt(currentUTCTimestamp);
    originalNote.setContent(aiResult.updatedParentContent);
    entityPersister.save(originalNote);

    noteService.deleteOrphanImagesForPersistedContent(newNote);
    noteService.deleteOrphanImagesForPersistedContent(originalNote);

    return noteRealmService.build(newNote, user);
  }
}
