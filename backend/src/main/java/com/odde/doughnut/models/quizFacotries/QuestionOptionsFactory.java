package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.EntityWithId;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import java.util.List;
import java.util.stream.Collectors;

public interface QuestionOptionsFactory<T extends EntityWithId> {
  T generateAnswer();

  List<T> generateFillingOptions();

  default List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    return List.of();
  }

  default Link getCategoryLink() {
    return null;
  }

  default String generateOptions(Randomizer randomizer) {
    List<T> options = null;
    T answerNote = generateAnswer();
    if (answerNote != null) {
      List<T> fillingOptions = generateFillingOptions();
      if (!fillingOptions.isEmpty()) {
        fillingOptions.add(answerNote);
        options = fillingOptions;
      }
    }
    if (options == null) return null;
    return randomizer.shuffle(options).stream()
        .map(T::getId)
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }
}
