package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.LinkCreation;
import com.odde.doughnut.controllers.json.NoteRealm;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.LinkModel;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
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

  @GetMapping("/{link}")
  public Link show(@PathVariable("link") Link link) throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(link);
    return link;
  }

  @PostMapping(value = "/{link}")
  @Transactional
  public NoteRealm updateLink(Link link, @RequestBody LinkCreation linkCreation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(link);
    link.setLinkType(linkCreation.linkType);
    modelFactoryService.save(link);
    return getNoteRealm(link, currentUser.getEntity(), linkCreation.fromTargetPerspective);
  }

  @PostMapping(value = "/{link}/{perspective}/delete")
  @Transactional
  public NoteRealm deleteLink(@PathVariable Link link, @PathVariable String perspective)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(link);
    LinkModel linkModel = modelFactoryService.toLinkModel(link);
    linkModel.destroy();
    return getNoteRealm(link, currentUser.getEntity(), perspective.equals("tview"));
  }

  @PostMapping(value = "/create/{sourceNote}/{targetNote}")
  @Transactional
  public NoteRealm linkNoteFinalize(
      @PathVariable Note sourceNote,
      @PathVariable Note targetNote,
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
    Link link =
        Link.createLink(
            sourceNote,
            targetNote,
            user,
            linkCreation.linkType,
            testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.save(link);

    Note note =
        sourceNote.buildChildNote(
            user, testabilitySettings.getCurrentUTCTimestamp(), ":" + linkCreation.linkType.label);
    note.setTargetNote(targetNote);
    modelFactoryService.save(note);

    return getNoteRealm(link, user, linkCreation.fromTargetPerspective);
  }

  private NoteRealm getNoteRealm(Link link, User user, Boolean fromTargetPerspective) {
    Note note = fromTargetPerspective ? link.getTargetNote() : link.getSourceNote();
    Note nn = modelFactoryService.entityManager.find(Note.class, note.getId());
    return new NoteViewer(user, nn).toJsonObject();
  }
}
