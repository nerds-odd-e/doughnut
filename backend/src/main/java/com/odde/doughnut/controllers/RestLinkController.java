
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewedByUser;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.LinkModel;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/links")
class RestLinkController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestLinkController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
  }

  @GetMapping("/{link}")
  public LinkViewedByUser show(@PathVariable("link") Link link) throws NoAccessRightException {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertReadAuthorization(link.getSourceNote());
    return LinkViewedByUser.from(link, user);
  }

  static class LinkRequest {
    public Integer typeId;
    public Boolean moveUnder;
    public Boolean asFirstChild;
  }

  @PostMapping(value = "/{link}")
  @Transactional
  public RedirectToNoteResponse updateLink(Link link, @RequestBody LinkRequest linkRequest) throws NoAccessRightException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(link.getSourceNote());
    link.setTypeId(linkRequest.typeId);
    modelFactoryService.linkRepository.save(link);
    return new RedirectToNoteResponse(link.getSourceNote().getId());
  }

  @PostMapping(value = "/{link}/delete")
  @Transactional
  public RedirectToNoteResponse deleteLink(Link link) throws NoAccessRightException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(link.getSourceNote());
    LinkModel linkModel = modelFactoryService.toLinkModel(link);
    linkModel.destroy();
    return new RedirectToNoteResponse(link.getSourceNote().getId());
  }

  @PostMapping(value = "/create/{sourceNote}/{targetNote}")
  @Transactional
  public Integer linkNoteFinalize(@PathVariable Note sourceNote, @PathVariable Note targetNote, @Valid @RequestBody  LinkRequest linkRequest) throws NoAccessRightException, CyclicLinkDetectedException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(sourceNote);
    currentUserFetcher.getUser().getAuthorization().assertReadAuthorization(targetNote);
    if (linkRequest != null && linkRequest.moveUnder != null && linkRequest.moveUnder) {
      currentUserFetcher.getUser().getAuthorization().assertAuthorization(targetNote);
      modelFactoryService.toNoteMotionModel(sourceNote, targetNote, linkRequest.asFirstChild).execute();
    }
    Link link = new Link();
    link.setSourceNote(sourceNote);
    link.setTargetNote(targetNote);
    link.setTypeId(linkRequest.typeId);
    link.setUser(currentUserFetcher.getUser().getEntity());
    modelFactoryService.linkRepository.save(link);
    return link.getId();
  }

  class LinkStatistics {
    @Getter
    @Setter
    private ReviewPoint reviewPoint;
    @Getter
    @Setter
    private Link link;

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
