package com.odde.doughnut.controllers.json;

import lombok.Getter;
import org.springframework.lang.Nullable;

public class QuizQuestionContestResult {
  @Getter @Nullable public QuizQuestion newQuizQuestion;
  @Getter public String reason;
  @Getter public Boolean legitimated = false;
}
