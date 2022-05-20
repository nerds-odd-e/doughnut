package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import lombok.Getter;
import lombok.Setter;

public class ReviewPointWithReviewSetting {
  @Getter @Setter private ReviewPoint reviewPoint;
  @Getter @Setter private ReviewSetting reviewSetting;

  public static ReviewPointWithReviewSetting from(ReviewPoint reviewPoint) {
    ReviewPointWithReviewSetting result = new ReviewPointWithReviewSetting();
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
