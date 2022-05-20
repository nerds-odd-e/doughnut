package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.ReviewPointWithReviewSetting;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
  public ReviewPointWithReviewSetting show(@PathVariable("reviewPoint") ReviewPoint reviewPoint)
      throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(reviewPoint);
    return ReviewPointWithReviewSetting.from(reviewPoint, user);
  }

  @PostMapping(path = "/{reviewPoint}/remove")
  public Integer removeFromRepeating(ReviewPoint reviewPoint) {
    reviewPoint.setRemovedFromReview(true);
    modelFactoryService.reviewPointRepository.save(reviewPoint);
    return reviewPoint.getId();
  }
}
