package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.Thingy;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionOptionsFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionRawJsonFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SecondaryReviewPointsFactory;
import java.util.List;

record QuizQuestionDirector(
    QuizQuestionEntity.QuestionType questionType, QuizQuestionServant servant) {

  public QuizQuestionEntity invoke(Thing thing) throws QuizQuestionNotPossibleException {
    return buildAQuestionOfType(questionType, thing, servant);
  }

  private QuizQuestionEntity buildAQuestionOfType(
      QuizQuestionEntity.QuestionType questionType, Thing thing, QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    QuizQuestionFactory quizQuestionFactory = questionType.factory.apply(thing, servant);

    quizQuestionFactory.validatePossibility();

    QuizQuestionEntity quizQuestion = new QuizQuestionEntity();
    quizQuestion.setQuestionType(questionType);

    if (quizQuestionFactory instanceof QuestionRawJsonFactory rawJsonFactory) {
      rawJsonFactory.generateRawJsonQuestion(quizQuestion);
    }

    if (quizQuestionFactory instanceof QuestionOptionsFactory optionsFactory) {
      Thingy answerNote = optionsFactory.generateAnswer();
      if (answerNote == null) {
        throw new QuizQuestionNotPossibleException();
      }
      List<? extends Thingy> options = optionsFactory.generateFillingOptions();
      if (options.size() < optionsFactory.minimumOptionCount() - 1) {
        throw new QuizQuestionNotPossibleException();
      }
      quizQuestion.setChoicesAndRightAnswer(answerNote, options, servant.randomizer);
    }

    if (quizQuestionFactory instanceof SecondaryReviewPointsFactory secondaryReviewPointsFactory) {
      quizQuestion.setViceReviewPoints(secondaryReviewPointsFactory.getViceReviewPoints());
      quizQuestion.setCategoryLink(secondaryReviewPointsFactory.getCategoryLink());
    }
    return quizQuestion;
  }
}
