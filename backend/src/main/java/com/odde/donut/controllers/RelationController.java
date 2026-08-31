package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteMotionService;
import com.odde.donut.services.NoteRealmService;
import com.odde.donut.services.ResolvedWikiLinkService;
import com.odde.donut.services.WikiLinkRewriteService;
import com.odde.donut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
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
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final ResolvedWikiLinkService resolvedWikiLinkService;
  private final TestabilitySettings testabilitySettings;

  public RelationController(
      NoteMotionService noteMotionService,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService,
      WikiLinkRewriteService wikiLinkRewriteService,
      ResolvedWikiLinkService resolvedWikiLinkService,
      TestabilitySettings testabilitySettings) {
    this.noteMotionService = noteMotionService;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.resolvedWikiLinkService = resolvedWikiLinkService;
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
    Notebook oldNotebook = sourceNote.getNotebook();
    Notebook targetNotebook = targetFolder.getNotebook();
    noteMotionService.executeMoveIntoFolder(sourceNote, targetFolder);
    User user = authorizationService.getCurrentUser();
    wikiLinkRewriteService.rewriteWikiLinksForCrossNotebookMove(
        sourceNote,
        oldNotebook,
        targetNotebook,
        testabilitySettings.getCurrentUTCTimestamp(),
        user);
    refreshCardinalityAcrossMovedNotebooks(oldNotebook, targetNotebook, user);
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
    noteMotionService.executeMoveToNotebookRoot(sourceNote, targetNotebook);
    User user = authorizationService.getCurrentUser();
    wikiLinkRewriteService.rewriteWikiLinksForCrossNotebookMove(
        sourceNote,
        oldNotebook,
        targetNotebook,
        testabilitySettings.getCurrentUTCTimestamp(),
        user);
    refreshCardinalityAcrossMovedNotebooks(oldNotebook, targetNotebook, user);
    return List.of(noteRealmService.build(sourceNote, user));
  }

  /**
   * A note changing notebooks can add or remove a title/alias candidate from either notebook's
   * Portable-path resolution scope. Re-resolve both, unrelated to the specific referrer rewrite
   * {@link WikiLinkRewriteService#rewriteWikiLinksForCrossNotebookMove} already performed. No-op
   * when the note stayed in the same notebook.
   */
  private void refreshCardinalityAcrossMovedNotebooks(
      Notebook oldNotebook, Notebook targetNotebook, User user) {
    Integer oldNotebookId = oldNotebook != null ? oldNotebook.getId() : null;
    if (Objects.equals(oldNotebookId, targetNotebook.getId())) {
      return;
    }
    if (oldNotebook != null) {
      resolvedWikiLinkService.refreshNotebookScope(oldNotebook, user);
    }
    resolvedWikiLinkService.refreshNotebookScope(targetNotebook, user);
  }
}
