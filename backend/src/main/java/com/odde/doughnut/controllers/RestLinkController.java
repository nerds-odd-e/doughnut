package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.LinkCreation;
import com.odde.doughnut.entities.json.NoteRealm;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.LinkModel;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import java.sql.Timestamp;
import javax.validation.Valid;
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

  private Timestamp currentUTCTimestamp;
  private UserModel currentUser;

  public RestLinkController(
      ModelFactoryService modelFactoryService,
      Timestamp currentUTCTimestamp,
      UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.currentUser = currentUser;
  }

  @GetMapping("/{link}")
  public Link show(@PathVariable("link") Link link) throws NoAccessRightException {
    currentUser.assertReadAuthorization(link);
    return link;
  }

  @PostMapping(value = "/{link}")
  @Transactional
  public NoteRealm updateLink(Link link, @RequestBody LinkCreation linkCreation)
      throws NoAccessRightException {
    currentUser.assertAuthorization(link);
    link.setLinkType(linkCreation.linkType);
    modelFactoryService.linkRepository.save(link);
    return getNoteRealm(link, currentUser.getEntity(), linkCreation.fromTargetPerspective);
  }

  private NoteRealm getNoteRealm(Link link, User user, Boolean fromTargetPerspective) {
    Note note = fromTargetPerspective ? link.getTargetNote() : link.getSourceNote();
    return new NoteViewer(user, note).toJsonObject();
  }

  @PostMapping(value = "/{link}/{perspective}/delete")
  @Transactional
  public NoteRealm deleteLink(@PathVariable Link link, @PathVariable String perspective)
      throws NoAccessRightException {
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
      throws NoAccessRightException, CyclicLinkDetectedException, BindException {
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
        Link.createLink(sourceNote, targetNote, user, linkCreation.linkType, currentUTCTimestamp);
    modelFactoryService.linkRepository.save(link);
    return getNoteRealm(link, user, linkCreation.fromTargetPerspective);
  }
}
