package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.annotations.JsonUseIdInsteadOfReviewPoint;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

public class RepetitionForUser {

  @JsonUseIdInsteadOfReviewPoint @Getter @Setter private ReviewPoint reviewPoint;
  @Getter @Setter private Optional<QuizQuestionViewedByUser> quizQuestion;
  @Getter @Setter private Integer toRepeatCount;
}
