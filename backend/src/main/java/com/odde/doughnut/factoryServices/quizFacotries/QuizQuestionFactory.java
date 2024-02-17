package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionOptionsFactory;
import java.util.List;

public interface QuizQuestionFactory {
  default void validatePossibility() throws QuizQuestionNotPossibleException {}

  QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException;

  default QuizQuestionEntity buildQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {

    QuizQuestionEntity quizQuestion = buildQuizQuestionObj(servant);

    validatePossibility();

    if (this instanceof QuestionOptionsFactory optionsFactory) {
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
    }

    return quizQuestion;
  }
}
