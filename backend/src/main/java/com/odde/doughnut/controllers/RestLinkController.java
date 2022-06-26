package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkCreation;
import com.odde.doughnut.entities.json.NotesBulk;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.LinkModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import javax.annotation.Resource;
import javax.validation.Valid;
import lombok.Getter;
import lombok.Setter;
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
  private final CurrentUserFetcher currentUserFetcher;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestLinkController(
      ModelFactoryService modelFactoryService,
      CurrentUserFetcher currentUserFetcher,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/{link}")
  public Link show(@PathVariable("link") Link link) throws NoAccessRightException {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertReadAuthorization(link);
    return link;
  }

  @PostMapping(value = "/{link}")
  @Transactional
  public NotesBulk updateLink(Link link, @RequestBody LinkCreation linkCreation)
      throws NoAccessRightException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(link);
    link.setLinkType(linkCreation.linkType);
    modelFactoryService.linkRepository.save(link);
    return NotesBulk.jsonNoteRealm(link.getSourceNote(), currentUserFetcher.getUser());
  }

  @PostMapping(value = "/{link}/delete")
  @Transactional
  public Integer deleteLink(Link link) throws NoAccessRightException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(link);
    LinkModel linkModel = modelFactoryService.toLinkModel(link);
    linkModel.destroy();
    return link.getId();
  }

  @PostMapping(value = "/create/{sourceNote}/{targetNote}")
  @Transactional
  public NotesBulk linkNoteFinalize(
      @PathVariable Note sourceNote,
      @PathVariable Note targetNote,
      @RequestBody @Valid LinkCreation linkCreation,
      BindingResult bindingResult)
      throws NoAccessRightException, CyclicLinkDetectedException, BindException {
    if (bindingResult.hasErrors()) throw new BindException(bindingResult);
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(sourceNote);
    currentUserFetcher.getUser().getAuthorization().assertReadAuthorization(targetNote);
    if (linkCreation != null && linkCreation.moveUnder != null && linkCreation.moveUnder) {
      currentUserFetcher.getUser().getAuthorization().assertAuthorization(targetNote);
      modelFactoryService
          .toNoteMotionModel(sourceNote, targetNote, linkCreation.asFirstChild)
          .execute();
    }
    Link link =
        Link.createLink(
            sourceNote,
            targetNote,
            currentUserFetcher.getUser().getEntity(),
            linkCreation.linkType,
            testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.linkRepository.save(link);
    return NotesBulk.jsonNoteRealm(link.getSourceNote(), currentUserFetcher.getUser());
  }

  class LinkStatistics {
    @Getter @Setter private ReviewPoint reviewPoint;
    @Getter @Setter private Link link;
  }

  @GetMapping("/{link}/statistics")
  public LinkStatistics statistics(@PathVariable("link") Link link) throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(link);
    LinkStatistics statistics = new LinkStatistics();
    statistics.setReviewPoint(user.getReviewPointFor(link));
    statistics.setLink(link);
    return statistics;
  }
}
