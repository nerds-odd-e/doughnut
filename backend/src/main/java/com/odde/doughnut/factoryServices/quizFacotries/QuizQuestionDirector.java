package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.Thingy;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionOptionsFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionRawJsonFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SecondaryReviewPointsFactory;
import com.odde.doughnut.services.ai.OpenAIConfig;
import java.util.List;

public record QuizQuestionDirector(
    QuizQuestionEntity.QuestionType questionType, QuizQuestionServant servant) {

  public QuizQuestionEntity invoke(Thing thing, OpenAIConfig config)
      throws QuizQuestionNotPossibleException {
    return buildAQuestionOfType(questionType, thing, servant, config);
  }

  private QuizQuestionEntity buildAQuestionOfType(
      QuizQuestionEntity.QuestionType questionType,
      Thing thing,
      QuizQuestionServant servant,
      OpenAIConfig config)
      throws QuizQuestionNotPossibleException {
    QuizQuestionFactory quizQuestionFactory = questionType.factory.apply(thing, servant);

    quizQuestionFactory.validatePossibility();

    QuizQuestionEntity quizQuestion = new QuizQuestionEntity();
    quizQuestion.setThing(thing);
    quizQuestion.setQuestionType(questionType);

    if (quizQuestionFactory instanceof QuestionRawJsonFactory rawJsonFactory) {
      rawJsonFactory.generateRawJsonQuestion(quizQuestion, config);
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
      quizQuestion.setCategoryLink(secondaryReviewPointsFactory.getCategoryLink());
    }
    return quizQuestion;
  }
}
