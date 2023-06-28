package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import java.util.List;

public class NullQuizQuestionPresenter implements QuizQuestionPresenter {

  public NullQuizQuestionPresenter(QuizQuestionEntity quizQuestion) {}

  @Override
  public String mainTopic() {
    return "";
  }

  @Override
  public List<Note> knownRightAnswers() {
    return null;
  }

  @Override
  public String instruction() {
    return "";
  }
}
