package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.LinkCreation;
import com.odde.doughnut.controllers.dto.NoteMoveDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
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

  private UserModel currentUser;
  private final NoteMotionService noteMotionService;

  public LinkController(
      ModelFactoryService modelFactoryService,
      TestabilitySettings testabilitySettings,
      UserModel currentUser,
      NoteMotionService noteMotionService) {
    this.modelFactoryService = modelFactoryService;
    this.testabilitySettings = testabilitySettings;
    this.currentUser = currentUser;
    this.noteMotionService = noteMotionService;
  }

  @PostMapping(value = "/{link}")
  @Transactional
  public List<NoteRealm> updateLink(
      @PathVariable @Schema(type = "integer") Note link, @RequestBody LinkCreation linkCreation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(link);
    link.setLinkType(linkCreation.linkType);
    modelFactoryService.save(link);
    return getNoteRealm(link, currentUser.getEntity());
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
    currentUser.assertAuthorization(sourceNote);
    currentUser.assertAuthorization(targetNote);
    noteMotionService.executeMoveUnder(sourceNote, targetNote, noteMoveDTO.asFirstChild);
    User user = currentUser.getEntity();
    return List.of(
        new NoteViewer(user, sourceNote).toJsonObject(),
        new NoteViewer(user, targetNote).toJsonObject());
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
    currentUser.assertAuthorization(sourceNote);
    currentUser.assertReadAuthorization(targetNote);
    User user = currentUser.getEntity();
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
    return List.of(
        new NoteViewer(user, link).toJsonObject(),
        new NoteViewer(user, nt).toJsonObject(),
        new NoteViewer(user, np).toJsonObject());
  }
}
