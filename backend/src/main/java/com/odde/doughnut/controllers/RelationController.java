package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.RelationshipCreation;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.RelationshipNotePlacement;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteMotionService;
import com.odde.doughnut.services.NoteRealmService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relations")
class RelationController {
  private final NoteService noteService;

  private final TestabilitySettings testabilitySettings;

  private final NoteMotionService noteMotionService;
  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;

  public RelationController(
      NoteService noteService,
      TestabilitySettings testabilitySettings,
      NoteMotionService noteMotionService,
      AuthorizationService authorizationService,
      NoteRealmService noteRealmService) {
    this.noteService = noteService;
    this.testabilitySettings = testabilitySettings;
    this.noteMotionService = noteMotionService;
    this.authorizationService = authorizationService;
    this.noteRealmService = noteRealmService;
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
    noteMotionService.executeMoveToNotebookRoot(sourceNote, targetNotebook);
    User user = authorizationService.getCurrentUser();
    return List.of(noteRealmService.build(sourceNote, user));
  }

  @PostMapping(value = "/create/{sourceNote}/{targetNote}")
  @Transactional
  public List<NoteRealm> addRelationshipFinalize(
      @PathVariable @Schema(type = "integer") Note sourceNote,
      @PathVariable @Schema(type = "integer") Note targetNote,
      @RequestBody @Valid RelationshipCreation relationshipCreation,
      BindingResult bindingResult)
      throws UnexpectedNoAccessRightException, CyclicLinkDetectedException, BindException {
    if (bindingResult.hasErrors()) throw new BindException(bindingResult);
    authorizationService.assertAuthorization(sourceNote);
    authorizationService.assertReadAuthorization(targetNote);
    User user = authorizationService.getCurrentUser();
    RelationshipNotePlacement placement =
        relationshipCreation.relationshipNotePlacement != null
            ? relationshipCreation.relationshipNotePlacement
            : RelationshipNotePlacement.RELATIONS_SUBFOLDER;
    Note relation =
        noteService.createRelationship(
            sourceNote,
            targetNote,
            user,
            relationshipCreation.relationType,
            testabilitySettings.getCurrentUTCTimestamp(),
            placement);

    return List.of(
        noteRealmService.build(relation, user), noteRealmService.build(sourceNote, user));
  }
}
