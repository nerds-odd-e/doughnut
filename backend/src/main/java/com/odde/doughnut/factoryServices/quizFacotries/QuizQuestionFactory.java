package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionOptionsFactory;
import java.util.List;

public interface QuizQuestionFactory {

  default QuizQuestionEntity buildQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {

    if (this instanceof QuestionOptionsFactory optionsFactory) {
      QuizQuestionEntity quizQuestion = optionsFactory.buildQuizQuestionObj(servant);
      optionsFactory.validateBasicPossibility();
      QuizQuestionWithNoteChoices qq = (QuizQuestionWithNoteChoices) quizQuestion;
      Note answerNote = optionsFactory.generateAnswer(servant);
      if (answerNote == null) {
        throw new QuizQuestionNotPossibleException();
      }
      List<? extends Note> options = optionsFactory.generateFillingOptions(servant);
      if (options.size() < optionsFactory.minimumOptionCount() - 1) {
        throw new QuizQuestionNotPossibleException();
      }
      qq.setChoicesAndRightAnswer(answerNote, options, servant.randomizer);
      return quizQuestion;
    }
    return null;
  }
}
