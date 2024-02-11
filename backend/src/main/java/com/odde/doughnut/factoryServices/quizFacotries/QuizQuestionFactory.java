package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionOptionsFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SecondaryReviewPointsFactory;
import java.util.List;

public interface QuizQuestionFactory {
  default void validatePossibility() throws QuizQuestionNotPossibleException {}

  QuizQuestionEntity buildQuizQuestion() throws QuizQuestionNotPossibleException;

  default QuizQuestionEntity buildQuizQuestion(QuizQuestionServant servant1)
      throws QuizQuestionNotPossibleException {

    QuizQuestionEntity quizQuestion = buildQuizQuestion();

    validatePossibility();

    if (this instanceof QuestionOptionsFactory optionsFactory) {
      Note answerNote = optionsFactory.generateAnswer();
      if (answerNote == null) {
        throw new QuizQuestionNotPossibleException();
      }
      List<? extends Note> options = optionsFactory.generateFillingOptions();
      if (options.size() < optionsFactory.minimumOptionCount() - 1) {
        throw new QuizQuestionNotPossibleException();
      }
      quizQuestion.setChoicesAndRightAnswer(answerNote, options, servant1.randomizer);
    }

    if (this instanceof SecondaryReviewPointsFactory secondaryReviewPointsFactory) {
      quizQuestion.setCategoryLink(secondaryReviewPointsFactory.getCategoryLink());
    }
    return quizQuestion;
  }
}
