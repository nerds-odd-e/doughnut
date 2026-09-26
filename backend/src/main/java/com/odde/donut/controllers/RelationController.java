package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteMoveService;
import com.odde.donut.services.NoteRealmService;
import com.odde.donut.services.NoteReferenceService;
import com.odde.donut.services.notebookGit.RelationReduceService;
import com.odde.donut.services.notebookGit.WebNoteEditService;
import com.odde.donut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relations")
class RelationController {
  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;
  private final TestabilitySettings testabilitySettings;
  private final WebNoteEditService webNoteEditService;
  private final NoteMoveService noteMoveService;
  private final RelationReduceService relationReduceService;
  private final NoteReferenceService noteReferenceService;

  public RelationController(
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService,
      TestabilitySettings testabilitySettings,
      WebNoteEditService webNoteEditService,
      NoteMoveService noteMoveService,
      RelationReduceService relationReduceService,
      NoteReferenceService noteReferenceService) {
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
    this.testabilitySettings = testabilitySettings;
    this.webNoteEditService = webNoteEditService;
    this.noteMoveService = noteMoveService;
    this.relationReduceService = relationReduceService;
    this.noteReferenceService = noteReferenceService;
  }

  @PostMapping(value = "/move-to-folder/{sourceNote}/{targetFolder}")
  public List<NoteRealm> moveNoteToFolder(
      @PathVariable @Schema(type = "integer") Note sourceNote,
      @PathVariable @Schema(type = "integer") Folder targetFolder)
      throws UnexpectedNoAccessRightException {
    Notebook targetNotebook = targetFolder.getNotebook();
    authorizationService.assertAuthorization(sourceNote);
    authorizationService.assertAuthorization(targetNotebook);
    Function<Timestamp, Consumer<Note>> move =
        isSameNotebook(sourceNote, targetNotebook)
            ? now -> noteMoveService.sameNotebookMoveIntoFolder(targetFolder.getId(), now)
            : now ->
                noteMoveService.crossNotebookMoveIntoFolder(
                    targetFolder.getId(), targetNotebook, now);
    return List.of(webMove(sourceNote, targetNotebook, move));
  }

  @PostMapping(value = "/move-to-notebook-root/{sourceNote}")
  public List<NoteRealm> moveNoteToNotebookRoot(
      @PathVariable @Schema(type = "integer") Note sourceNote)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(sourceNote);
    authorizationService.assertAuthorization(sourceNote.getNotebook());
    return List.of(
        webMove(sourceNote, sourceNote.getNotebook(), noteMoveService::sameNotebookMoveToRoot));
  }

  @PostMapping(value = "/move-to-notebook-root/{sourceNote}/{targetNotebook}")
  public List<NoteRealm> moveNoteToNotebookRootInNotebook(
      @PathVariable @Schema(type = "integer") Note sourceNote,
      @PathVariable @Schema(type = "integer") Notebook targetNotebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(sourceNote);
    authorizationService.assertAuthorization(targetNotebook);
    Function<Timestamp, Consumer<Note>> move =
        isSameNotebook(sourceNote, targetNotebook)
            ? noteMoveService::sameNotebookMoveToRoot
            : now -> noteMoveService.crossNotebookMoveToRoot(targetNotebook, now);
    return List.of(webMove(sourceNote, targetNotebook, move));
  }

  @PostMapping(value = "/{relationNote}/reduce-to-source-property")
  public NoteRealm reduceToSourceProperty(@PathVariable @Schema(type = "integer") Note relationNote)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(relationNote);
    return relationReduceService.reduceToSourceProperty(relationNote);
  }

  private static boolean isSameNotebook(Note sourceNote, Notebook targetNotebook) {
    return Objects.equals(sourceNote.getNotebook().getId(), targetNotebook.getId());
  }

  private NoteRealm webMove(
      Note sourceNote, Notebook targetNotebook, Function<Timestamp, Consumer<Note>> mutationFactory)
      throws UnexpectedNoAccessRightException {
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    Integer sourceNotebookId = sourceNote.getNotebook().getId();
    Note moved =
        webNoteEditService.edit(
            sourceNote.getId(),
            sourceNotebookId,
            noteReferenceService.notebooksToLock(
                List.of(sourceNote),
                authorizationService.getCurrentUser(),
                sourceNotebookId,
                targetNotebook.getId()),
            mutationFactory.apply(now),
            note -> "Move note: " + note.getTitle(),
            now);
    return noteRealmService.build(moved, authorizationService.getCurrentUser());
  }
}
