
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.NoteViewedByUser;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-points")
class RestReviewPointController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestReviewPointController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
  }

  class ReviewPointViewedByUser {
    @Getter
    @Setter
    private ReviewPoint reviewPoint;
    @Getter
    @Setter
    private NoteViewedByUser sourceNoteViewedByUser;

  }

  @GetMapping("/{reviewPoint}")
  public ReviewPointViewedByUser show(@PathVariable("reviewPoint") ReviewPoint reviewPoint) throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    //user.getAuthorization().assertAuthorization(reviewPoint);
    ReviewPointViewedByUser result = new ReviewPointViewedByUser();
    result.setReviewPoint(reviewPoint);
    result.setSourceNoteViewedByUser(reviewPoint.getSourceNote().jsonObjectViewedBy(user.getEntity()));
    return result;
  }
}
