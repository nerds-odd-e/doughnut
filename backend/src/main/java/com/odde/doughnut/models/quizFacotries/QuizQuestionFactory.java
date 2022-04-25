package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import java.util.List;

public interface QuizQuestionFactory {
  default boolean isValidQuestion() {
    return true;
  }

  default List<Note> knownRightAnswers() {
    return null;
  }

  default List<Note> allWrongAnswers() {
    return null;
  }
}
