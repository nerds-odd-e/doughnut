package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteMotionService;
import com.odde.doughnut.services.NoteRealmService;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relations")
class RelationController {
  private final NoteMotionService noteMotionService;
  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final TestabilitySettings testabilitySettings;

  public RelationController(
      NoteMotionService noteMotionService,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService,
      WikiTitleCacheService wikiTitleCacheService,
      TestabilitySettings testabilitySettings) {
    this.noteMotionService = noteMotionService;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping(value = "/move-to-folder/{sourceNote}/{targetFolder}")
  @Transactional
  public List<NoteRealm> moveNoteToFolder(
      @PathVariable @Schema(type = "integer") Note sourceNote,
      @PathVariable @Schema(type = "integer") Folder targetFolder)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(sourceNote);
    authorizationService.assertAuthorization(targetFolder.getNotebook());
    noteMotionService.executeMoveIntoFolder(sourceNote, targetFolder);
    User user = authorizationService.getCurrentUser();
    return List.of(noteRealmService.build(sourceNote, user));
  }

  @PostMapping(value = "/move-to-notebook-root/{sourceNote}")
  @Transactional
  public List<NoteRealm> moveNoteToNotebookRoot(
      @PathVariable @Schema(type = "integer") Note sourceNote)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(sourceNote);
    authorizationService.assertAuthorization(sourceNote.getNotebook());
    noteMotionService.executeMoveToNotebookRoot(sourceNote, sourceNote.getNotebook());
    User user = authorizationService.getCurrentUser();
    return List.of(noteRealmService.build(sourceNote, user));
  }

  @PostMapping(value = "/move-to-notebook-root/{sourceNote}/{targetNotebook}")
  @Transactional
  public List<NoteRealm> moveNoteToNotebookRootInNotebook(
      @PathVariable @Schema(type = "integer") Note sourceNote,
      @PathVariable @Schema(type = "integer") Notebook targetNotebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(sourceNote);
    authorizationService.assertAuthorization(targetNotebook);
    Notebook oldNotebook = sourceNote.getNotebook();
    Integer oldNotebookId = oldNotebook != null ? oldNotebook.getId() : null;
    String oldNotebookName = oldNotebook != null ? oldNotebook.getName() : null;
    noteMotionService.executeMoveToNotebookRoot(sourceNote, targetNotebook);
    User user = authorizationService.getCurrentUser();
    if (!Objects.equals(oldNotebookId, targetNotebook.getId())) {
      Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
      wikiTitleCacheService.rewriteInboundWikiLinksForNotebookMove(
          sourceNote, targetNotebook.getName(), currentUTCTimestamp, user);
      wikiTitleCacheService.rewriteOutgoingWikiLinksForNotebookMove(
          sourceNote, oldNotebookName, currentUTCTimestamp, user);
    }
    return List.of(noteRealmService.build(sourceNote, user));
  }
}
