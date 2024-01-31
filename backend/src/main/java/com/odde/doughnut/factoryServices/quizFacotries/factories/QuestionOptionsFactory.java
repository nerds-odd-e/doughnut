package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import java.util.List;

public interface QuestionOptionsFactory {
  Note generateAnswer();

  List<Note> generateFillingOptions();

  default int minimumOptionCount() {
    return 2;
  }
  ;
}
