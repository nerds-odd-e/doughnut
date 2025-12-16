package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.LinkCreation;
import com.odde.doughnut.controllers.dto.NoteMoveDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
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
@RequestMapping("/api/links")
class LinkController {
  private final EntityPersister entityPersister;
  private final NoteService noteService;

  private final TestabilitySettings testabilitySettings;

  private final NoteMotionService noteMotionService;
  private final AuthorizationService authorizationService;

  public LinkController(
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

  @PostMapping(value = "/{link}")
  @Transactional
  public List<NoteRealm> updateLink(
      @PathVariable @Schema(type = "integer") Note link, @RequestBody LinkCreation linkCreation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(link);
    link.setRelationType(linkCreation.relationType);
    entityPersister.save(link);
    return getNoteRealm(link, authorizationService.getCurrentUser());
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
  public List<NoteRealm> linkNoteFinalize(
      @PathVariable @Schema(type = "integer") Note sourceNote,
      @PathVariable @Schema(type = "integer") Note targetNote,
      @RequestBody @Valid LinkCreation linkCreation,
      BindingResult bindingResult)
      throws UnexpectedNoAccessRightException, CyclicLinkDetectedException, BindException {
    if (bindingResult.hasErrors()) throw new BindException(bindingResult);
    authorizationService.assertAuthorization(sourceNote);
    authorizationService.assertReadAuthorization(targetNote);
    User user = authorizationService.getCurrentUser();
    Note link =
        noteService.createLink(
            sourceNote,
            targetNote,
            user,
            linkCreation.relationType,
            testabilitySettings.getCurrentUTCTimestamp());

    return getNoteRealm(link, user);
  }

  private List<NoteRealm> getNoteRealm(Note link, User user) {
    Note nt = entityPersister.find(Note.class, link.getTargetNote().getId());
    Note np = entityPersister.find(Note.class, link.getParent().getId());
    return List.of(link.toNoteRealm(user), nt.toNoteRealm(user), np.toNoteRealm(user));
  }
}
