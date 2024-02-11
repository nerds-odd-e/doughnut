package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public interface QuestionOptionsFactory {
  Note generateAnswer(QuizQuestionServant servant);

  List<? extends Note> generateFillingOptions(QuizQuestionServant servant);

  default int minimumOptionCount() {
    return 2;
  }
  ;
}
