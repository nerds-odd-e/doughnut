package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.SelfEvaluation;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
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

  @GetMapping("/{reviewPoint}")
  public ReviewPoint show(@PathVariable("reviewPoint") ReviewPoint reviewPoint)
      throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(reviewPoint);
    return reviewPoint;
  }

  @PostMapping(path = "/{reviewPoint}/remove")
  public ReviewPoint removeFromRepeating(ReviewPoint reviewPoint) {
    reviewPoint.setRemovedFromReview(true);
    modelFactoryService.reviewPointRepository.save(reviewPoint);
    return reviewPoint;
  }

  @PostMapping(path = "/{reviewPoint}/self-evaluate")
  @Transactional
  public ReviewPoint selfEvaluate(
      ReviewPoint reviewPoint, @RequestBody SelfEvaluation selfEvaluation) {
    if (reviewPoint == null || reviewPoint.getId() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The review point does not exist.");
    }
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    modelFactoryService
        .toReviewPointModel(reviewPoint)
        .evaluate(selfEvaluation.selfEvaluation.adjustment);
    return reviewPoint;
  }
}
