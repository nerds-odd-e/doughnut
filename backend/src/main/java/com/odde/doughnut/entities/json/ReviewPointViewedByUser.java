package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;

public class ReviewPointViewedByUser {
  @Getter @Setter private ReviewPoint reviewPoint;
  @Getter @Setter private ReviewSetting reviewSetting;

  public static ReviewPointViewedByUser from(ReviewPoint reviewPoint, UserModel user) {
    ReviewPointViewedByUser result = new ReviewPointViewedByUser();
    result.setReviewPoint(reviewPoint);
    if (reviewPoint.getNote() != null) {
      result.setReviewSetting(getReviewSetting(reviewPoint.getNote()));
    }
    return result;
  }

  private static ReviewSetting getReviewSetting(Note note) {
    if (note == null) {
      return null;
    }
    ReviewSetting reviewSetting = note.getMasterReviewSetting();
    if (reviewSetting == null) {
      reviewSetting = new ReviewSetting();
    }
    return reviewSetting;
  }
}
