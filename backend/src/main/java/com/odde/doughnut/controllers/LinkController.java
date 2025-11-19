package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.LinkCreation;
import com.odde.doughnut.controllers.dto.NoteMoveDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteMotionService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
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
  private final ModelFactoryService modelFactoryService;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private CurrentUser currentUser;
  private final NoteMotionService noteMotionService;
  private final AuthorizationService authorizationService;

  public LinkController(
      ModelFactoryService modelFactoryService,
      TestabilitySettings testabilitySettings,
      CurrentUser currentUser,
      NoteMotionService noteMotionService,
      AuthorizationService authorizationService) {
    this.modelFactoryService = modelFactoryService;
    this.testabilitySettings = testabilitySettings;
    this.currentUser = currentUser;
    this.noteMotionService = noteMotionService;
    this.authorizationService = authorizationService;
  }

  @PostMapping(value = "/{link}")
  @Transactional
  public List<NoteRealm> updateLink(
      @PathVariable @Schema(type = "integer") Note link, @RequestBody LinkCreation linkCreation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(currentUser.getUser(), link);
    link.setLinkType(linkCreation.linkType);
    modelFactoryService.save(link);
    return getNoteRealm(link, currentUser.getUser());
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
    authorizationService.assertAuthorization(currentUser.getUser(), sourceNote);
    authorizationService.assertAuthorization(currentUser.getUser(), targetNote);
    noteMotionService.executeMoveUnder(sourceNote, targetNote, noteMoveDTO.asFirstChild);
    User user = currentUser.getUser();
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
    authorizationService.assertAuthorization(currentUser.getUser(), sourceNote);
    authorizationService.assertReadAuthorization(currentUser.getUser(), targetNote);
    User user = currentUser.getUser();
    Note link =
        modelFactoryService.createLink(
            sourceNote,
            targetNote,
            user,
            linkCreation.linkType,
            testabilitySettings.getCurrentUTCTimestamp());

    return getNoteRealm(link, user);
  }

  private List<NoteRealm> getNoteRealm(Note link, User user) {
    Note nt = modelFactoryService.entityManager.find(Note.class, link.getTargetNote().getId());
    Note np = modelFactoryService.entityManager.find(Note.class, link.getParent().getId());
    return List.of(link.toNoteRealm(user), nt.toNoteRealm(user), np.toNoteRealm(user));
  }
}
