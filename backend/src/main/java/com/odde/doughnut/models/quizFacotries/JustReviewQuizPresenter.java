package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import java.util.List;

public class JustReviewQuizPresenter implements QuizQuestionPresenter {
  public JustReviewQuizPresenter(QuizQuestion quizQuestion) {}

  @Override
  public String instruction() {
    return null;
  }

  @Override
  public String mainTopic() {
    return null;
  }

  @Override
  public List<Note> knownRightAnswers() {
    return null;
  }
}
