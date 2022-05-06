package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.annotations.JsonUseIdInsteadOfReviewPoint;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

public class RepetitionForUser {

  @JsonUseIdInsteadOfReviewPoint @Getter @Setter private ReviewPoint reviewPoint;
  @Getter @Setter @Nullable private QuizQuestionViewedByUser quizQuestion;
  @Getter @Setter private Integer toRepeatCount;
}
