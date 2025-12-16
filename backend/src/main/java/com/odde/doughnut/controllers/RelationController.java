package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteMoveDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.RelationshipCreation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteMotionService;
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
  private final EntityPersister entityPersister;
  private final NoteService noteService;

  private final TestabilitySettings testabilitySettings;

  private final NoteMotionService noteMotionService;
  private final AuthorizationService authorizationService;

  public RelationController(
      EntityPersister entityPersister,
      NoteService noteService,
      TestabilitySettings testabilitySettings,
      NoteMotionService noteMotionService,
      AuthorizationService authorizationService) {
    this.entityPersister = entityPersister;
    this.noteService = noteService;
    this.testabilitySettings = testabilitySettings;
    this.noteMotionService = noteMotionService;
    this.authorizationService = authorizationService;
  }

  @PostMapping(value = "/{relation}")
  @Transactional
  public List<NoteRealm> updateRelationship(
      @PathVariable @Schema(type = "integer") Note relation,
      @RequestBody RelationshipCreation relationshipCreation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(relation);
    relation.setRelationType(relationshipCreation.relationType);
    entityPersister.save(relation);
    return getNoteRealm(relation, authorizationService.getCurrentUser());
  }

  @PostMapping(value = "/move/{sourceNote}/{targetNote}")
  @Transactional
  public List<NoteRealm> moveNote(
      @PathVariable @Schema(type = "integer") Note sourceNote,
      @PathVariable @Schema(type = "integer") Note targetNote,
      @RequestBody @Valid NoteMoveDTO noteMoveDTO,
      BindingResult bindingResult)
      throws UnexpectedNoAccessRightException, BindException, CyclicLinkDetectedException {
    if (bindingResult.hasErrors()) throw new BindException(bindingResult);
    authorizationService.assertAuthorization(sourceNote);
    authorizationService.assertAuthorization(targetNote);
    noteMotionService.executeMoveUnder(sourceNote, targetNote, noteMoveDTO.asFirstChild);
    User user = authorizationService.getCurrentUser();
    return List.of(sourceNote.toNoteRealm(user), targetNote.toNoteRealm(user));
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
    Note relation =
        noteService.createRelationship(
            sourceNote,
            targetNote,
            user,
            relationshipCreation.relationType,
            testabilitySettings.getCurrentUTCTimestamp());

    return getNoteRealm(relation, user);
  }

  private List<NoteRealm> getNoteRealm(Note relation, User user) {
    Note nt = entityPersister.find(Note.class, relation.getTargetNote().getId());
    Note np = entityPersister.find(Note.class, relation.getParent().getId());
    return List.of(relation.toNoteRealm(user), nt.toNoteRealm(user), np.toNoteRealm(user));
  }
}
