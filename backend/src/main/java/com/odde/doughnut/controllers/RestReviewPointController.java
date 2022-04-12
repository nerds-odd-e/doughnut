package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.ReviewPointViewedByUser;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/review-points")
class RestReviewPointController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestReviewPointController(
      ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
  }

  @GetMapping("/{reviewPointId}")
  public ReviewPointViewedByUser show(@PathVariable("reviewPointId") Integer reviewPointId)
      throws NoAccessRightException {
    return modelFactoryService
        .reviewPointRepository
        .findById(reviewPointId)
        .map(
            reviewPoint -> {
              final UserModel user = currentUserFetcher.getUser();
              user.getAuthorization().assertAuthorization(reviewPoint);
              return ReviewPointViewedByUser.from(reviewPoint, user);
            })
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "The review point does not exist."));
  }

  @PostMapping(path = "/{reviewPoint}/remove")
  public Integer removeFromRepeating(ReviewPoint reviewPoint) {
    reviewPoint.setRemovedFromReview(true);
    modelFactoryService.reviewPointRepository.save(reviewPoint);
    return reviewPoint.getId();
  }
}
