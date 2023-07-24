package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.QuizQuestionEntity.QuestionType;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.AiAdvisorService;
import java.util.Optional;

public record QuizQuestionGenerator(
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
          new QuizQuestionDirector(questionType, servant).invoke(reviewPoint.getThing());
      quizQuestion.setReviewPoint(reviewPoint);
      return Optional.of(quizQuestion);
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
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
              quizQuestion.setThing(reviewPoint.getThing());
              quizQuestion.setReviewPoint(reviewPoint);
              return quizQuestion;
            });
  }
}
