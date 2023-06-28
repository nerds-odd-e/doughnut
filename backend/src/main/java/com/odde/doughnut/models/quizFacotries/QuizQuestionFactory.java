package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.services.AiAdvisorService;

public interface QuizQuestionFactory {
  default boolean isValidQuestion() throws QuizQuestionNotPossibleException {
    return true;
  }

  default void fillQuizQuestion(QuizQuestion quizQuestion, AiAdvisorService aiAdvisorService) {}
}
