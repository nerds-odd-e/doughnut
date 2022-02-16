
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewedByUser;
import com.odde.doughnut.entities.json.NotesBulk;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.LinkModel;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
    @NotNull
    public Integer typeId;
    public Boolean moveUnder;
    public Boolean asFirstChild;
  }

  @PostMapping(value = "/{link}")
  @Transactional
  public NotesBulk updateLink(Link link, @RequestBody LinkRequest linkRequest) throws NoAccessRightException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(link.getSourceNote());
    link.setTypeId(linkRequest.typeId);
    modelFactoryService.linkRepository.save(link);
    return NotesBulk.jsonNoteWithChildren(link.getSourceNote(), currentUserFetcher.getUser());
  }

  @PostMapping(value = "/{link}/delete")
  @Transactional
  public NotesBulk deleteLink(Link link) throws NoAccessRightException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(link.getSourceNote());
    LinkModel linkModel = modelFactoryService.toLinkModel(link);
    linkModel.destroy();
    return NotesBulk.jsonNoteWithChildren(link.getSourceNote(), currentUserFetcher.getUser());
  }

  @PostMapping(value = "/create/{sourceNote}/{targetNote}")
  @Transactional
  public NotesBulk linkNoteFinalize(@PathVariable Note sourceNote, @PathVariable Note targetNote, @RequestBody @Valid LinkRequest linkRequest, BindingResult bindingResult) throws NoAccessRightException, CyclicLinkDetectedException, BindException {
    if(bindingResult.hasErrors()) throw new BindException(bindingResult);
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
    return NotesBulk.jsonNoteWithChildren(link.getSourceNote(), currentUserFetcher.getUser());
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
