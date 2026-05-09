package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateContentDTO;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteRealmService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.NotebookService;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.function.Consumer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text_content")
class TextContentController {
  private final EntityPersister entityPersister;

  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteService noteService;
  private final NotebookService notebookService;

  public TextContentController(
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService,
      WikiTitleCacheService wikiTitleCacheService,
      NoteService noteService,
      NotebookService notebookService) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.noteService = noteService;
    this.notebookService = notebookService;
  }

  @PatchMapping(path = "/{note}/title")
  @Transactional
  public NoteRealm updateNoteTitle(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateTitleDTO titleDTO)
      throws UnexpectedNoAccessRightException {
    return updateNoteTitleAndReconcileIndexPointer(note, n -> n.setTitle(titleDTO.getNewTitle()));
  }

  @PatchMapping(path = "/{note}/content")
  @Transactional
  public NoteRealm updateNoteContent(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateContentDTO contentDTO)
      throws UnexpectedNoAccessRightException {
    return updateNote(note, n -> n.setContent(contentDTO.getContent()), true, true);
  }

  private NoteRealm updateNote(
      Note note,
      Consumer<Note> updateFunction,
      boolean refreshWikiTitleCache,
      boolean deleteOrphanImagesAfterSave)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    note.setUpdatedAt(currentUTCTimestamp);
    updateFunction.accept(note);
    entityPersister.save(note);
    if (deleteOrphanImagesAfterSave) {
      noteService.deleteOrphanImagesForPersistedContent(note);
    }
    if (refreshWikiTitleCache) {
      wikiTitleCacheService.refreshForNote(note, authorizationService.getCurrentUser());
    }
    return noteRealmService.build(note, authorizationService.getCurrentUser());
  }

  private NoteRealm updateNote(
      Note note, Consumer<Note> updateFunction, boolean refreshWikiTitleCache)
      throws UnexpectedNoAccessRightException {
    return updateNote(note, updateFunction, refreshWikiTitleCache, false);
  }

  private NoteRealm updateNoteTitleAndReconcileIndexPointer(
      Note note, Consumer<Note> updateFunction) throws UnexpectedNoAccessRightException {
    NoteRealm realm = updateNote(note, updateFunction, false);
    notebookService.reconcileNotebookIndexNotePointer(note.getNotebook().getId());
    return realm;
  }
}
