package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import java.util.List;

public interface QuestionOptionsFactory {
  Note generateAnswerNote();

  List<Note> generateFillingOptions();

  default List<Note> generateOptions() {
    Note answerNote = generateAnswerNote();
    if (answerNote == null) return null;
    List<Note> fillingOptions = generateFillingOptions();
    if (fillingOptions.isEmpty()) {
      return null;
    }
    fillingOptions.add(answerNote);
    return fillingOptions;
  }
}
