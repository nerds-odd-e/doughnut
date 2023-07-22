package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.QuizQuestionEntity;

public class SpellingQuizPresenter extends ClozeDescriptonQuizPresenter {
  public SpellingQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
  }

  @Override
  public boolean isAnswerCorrect(Answer answer) {
    return reviewPoint.getNote().matchAnswer(answer.getSpellingAnswer());
  }
}
