package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.QuizQuestionEntity.QuestionType;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionOptionsFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionRawJsonFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SecondaryReviewPointsFactory;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.AiAdvisorService;
import java.util.List;
import java.util.Optional;

public record QuizQuestionDirector(
    ReviewPoint reviewPoint,
    Randomizer randomizer,
    ModelFactoryService modelFactoryService,
    AiAdvisorService aiAdvisorService) {

  public Optional<QuizQuestionEntity> buildQuizQuestion(QuestionType questionType) {
    try {
      QuizQuestionServant servant =
          new QuizQuestionServant(
              reviewPoint.getUser(), randomizer, modelFactoryService, aiAdvisorService);
      QuizQuestionEntity quizQuestion =
          buildAQuestionOfType(questionType, reviewPoint.getThing(), servant);
      quizQuestion.setReviewPoint(reviewPoint);
      return Optional.of(quizQuestion);
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  private QuizQuestionEntity buildAQuestionOfType(
      QuestionType questionType, Thing thing, QuizQuestionServant servant)
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

  public QuizQuestionEntity buildRandomQuestion(Boolean aiQuestionTypeOnlyForReview) {
    return this.randomizer
        .shuffle(reviewPoint.availableQuestionTypes(aiQuestionTypeOnlyForReview))
        .stream()
        .map(this::buildQuizQuestion)
        .flatMap(Optional::stream)
        .findFirst()
        .orElseGet(
            () -> {
              QuizQuestionEntity quizQuestion = new QuizQuestionEntity();
              quizQuestion.setQuestionType(QuestionType.JUST_REVIEW);
              quizQuestion.setReviewPoint(reviewPoint);
              return quizQuestion;
            });
  }
}
