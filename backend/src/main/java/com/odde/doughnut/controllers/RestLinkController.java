package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.LinkCreation;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
class RestLinkController {
  private final ModelFactoryService modelFactoryService;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private UserModel currentUser;

  public RestLinkController(
      ModelFactoryService modelFactoryService,
      TestabilitySettings testabilitySettings,
      UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.testabilitySettings = testabilitySettings;
    this.currentUser = currentUser;
  }

  @PostMapping(value = "/{link}")
  @Transactional
  public NoteRealm updateLink(
      @PathVariable @Schema(type = "integer") Note link, @RequestBody LinkCreation linkCreation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(link);
    link.setLinkType(linkCreation.linkType);
    modelFactoryService.save(link);
    return getNoteRealm(link, currentUser.getEntity(), linkCreation.fromTargetPerspective);
  }

  @PostMapping(value = "/{link}/{perspective}/delete")
  @Transactional
  public NoteRealm deleteLink(
      @PathVariable @Schema(type = "integer") Note link, @PathVariable String perspective)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(link);
    modelFactoryService.remove(link);
    return getNoteRealm(link, currentUser.getEntity(), perspective.equals("tview"));
  }

  @PostMapping(value = "/create/{sourceNote}/{targetNote}")
  @Transactional
  public NoteRealm linkNoteFinalize(
      @PathVariable @Schema(type = "integer") Note sourceNote,
      @PathVariable @Schema(type = "integer") Note targetNote,
      @RequestBody @Valid LinkCreation linkCreation,
      BindingResult bindingResult)
      throws UnexpectedNoAccessRightException, CyclicLinkDetectedException, BindException {
    if (bindingResult.hasErrors()) throw new BindException(bindingResult);
    currentUser.assertAuthorization(sourceNote);
    currentUser.assertReadAuthorization(targetNote);
    if (linkCreation != null && linkCreation.moveUnder != null && linkCreation.moveUnder) {
      currentUser.assertAuthorization(targetNote);
      modelFactoryService
          .toNoteMotionModel(sourceNote, targetNote, linkCreation.asFirstChild)
          .execute();
    }
    User user = currentUser.getEntity();
    Note link =
        modelFactoryService.createLink(
            sourceNote,
            targetNote,
            user,
            linkCreation.linkType,
            testabilitySettings.getCurrentUTCTimestamp());

    return getNoteRealm(link, user, linkCreation.fromTargetPerspective);
  }

  private NoteRealm getNoteRealm(Note link, User user, Boolean fromTargetPerspective) {
    Note note = fromTargetPerspective ? link.getTargetNote() : link.getParent();
    Note nn = modelFactoryService.entityManager.find(Note.class, note.getId());
    return new NoteViewer(user, nn).toJsonObject();
  }
}
