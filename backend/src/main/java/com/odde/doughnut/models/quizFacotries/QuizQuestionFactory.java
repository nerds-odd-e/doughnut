package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
import java.util.List;

public interface QuizQuestionFactory {
  default boolean isValidQuestion() {
    return true;
  }

  default int minimumViceReviewPointCount() {
    return 0;
  }

  default List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    return List.of();
  }

  default List<Note> knownRightAnswers() {
    return null;
  }

  default List<Note> allWrongAnswers() {
    return null;
  }

  default Link getCategoryLink() {
    return null;
  }
}
