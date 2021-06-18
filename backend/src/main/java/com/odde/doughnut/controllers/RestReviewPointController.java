
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.NoteViewedByUser;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
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

  class LinkViewedByUser {
    @Getter
    @Setter
    private NoteViewedByUser sourceNoteViewedByUser;
    @Getter
    @Setter
    private String linkTypeLabel;
    @Getter
    @Setter
    private NoteViewedByUser targetNoteViewedByUser;
  }

  class ReviewPointViewedByUser {
    @Getter
    @Setter
    private ReviewPoint reviewPoint;
    @Getter
    @Setter
    private NoteViewedByUser noteViewedByUser;
    @Getter
    @Setter
    private LinkViewedByUser linkViewedByUser;

  }

  @GetMapping("/{reviewPoint}")
  public ReviewPointViewedByUser show(@PathVariable("reviewPoint") ReviewPoint reviewPoint) throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    //user.getAuthorization().assertAuthorization(reviewPoint);
    final User entity = user.getEntity();
    ReviewPointViewedByUser result = getReviewPointViewedByUser(reviewPoint, entity);
    return result;
  }

  @NotNull
  private ReviewPointViewedByUser getReviewPointViewedByUser(ReviewPoint reviewPoint, User entity) {
    ReviewPointViewedByUser result = new ReviewPointViewedByUser();
    result.setReviewPoint(reviewPoint);
    if (reviewPoint.getNote() != null) {
      result.setNoteViewedByUser(reviewPoint.getNote().jsonObjectViewedBy(entity));
    }
    else {
      LinkViewedByUser linkViewedByUser = new LinkViewedByUser();
      linkViewedByUser.setSourceNoteViewedByUser(reviewPoint.getLink().getSourceNote().jsonObjectViewedBy(entity));
      linkViewedByUser.setTargetNoteViewedByUser(reviewPoint.getLink().getTargetNote().jsonObjectViewedBy(entity));
      linkViewedByUser.setLinkTypeLabel(reviewPoint.getLink().getLinkTypeLabel());
      result.setLinkViewedByUser(linkViewedByUser);
    }
    return result;
  }
}
