package com.odde.doughnut.models.quizFacotries;

import java.util.List;

public interface QuestionOptionsFactory<T> {
  T generateAnswer();

  List<T> generateFillingOptions();

  default List<T> generateOptions() {
    T answerNote = generateAnswer();
    if (answerNote == null) return null;
    List<T> fillingOptions = generateFillingOptions();
    if (fillingOptions.isEmpty()) {
      return null;
    }
    fillingOptions.add(answerNote);
    return fillingOptions;
  }
}
