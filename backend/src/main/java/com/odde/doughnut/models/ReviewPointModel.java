package com.odde.doughnut.models;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionGenerator;
import com.odde.doughnut.services.AiAdvisorService;
import java.sql.Timestamp;
import java.util.List;

public record ReviewPointModel(ReviewPoint entity, ModelFactoryService modelFactoryService) {

  public ReviewPoint getEntity() {
    return entity;
  }

  public void initialReview(Timestamp currentUTCTimestamp, User user) {
    entity.setUser(user);
    entity.setInitialReviewedAt(currentUTCTimestamp);
    entity.setLastReviewedAt(currentUTCTimestamp);
    updateForgettingCurve(0);
  }

  public QuizQuestion generateAQuizQuestion(
      Randomizer randomizer, User user, AiAdvisorService aiAdvisorService) {
    QuizQuestionGenerator quizQuestionGenerator =
        getQuizQuestionGenerator(randomizer, aiAdvisorService);
    QuizQuestionEntity quizQuestionEntity =
        quizQuestionGenerator.generateAQuestionOfFirstPossibleType(
            shuffleAvailableQuestionTypes(
                entity, randomizer, user.getAiQuestionTypeOnlyForReview()));
    return modelFactoryService.toQuizQuestion(quizQuestionEntity, user);
  }

  private static List<QuizQuestionEntity.QuestionType> shuffleAvailableQuestionTypes(
      ReviewPoint entity, Randomizer randomizer, Boolean aiQuestionTypeOnlyForReview) {
    return randomizer.shuffle(entity.availableQuestionTypes(aiQuestionTypeOnlyForReview));
  }

  private QuizQuestionGenerator getQuizQuestionGenerator(
      Randomizer randomizer, AiAdvisorService aiAdvisorService) {
    return new QuizQuestionGenerator(
        entity.getUser(), entity.getNote(), randomizer, modelFactoryService, aiAdvisorService);
  }

  public void markAsRepeated(Timestamp currentUTCTimestamp, boolean successful) {
    entity.setRepetitionCount(entity.getRepetitionCount() + 1);
    if (successful) {
      entity.reviewedSuccessfully(currentUTCTimestamp);
    } else {
      entity.reviewFailed(currentUTCTimestamp);
    }
    this.modelFactoryService.save(entity);
  }

  public void updateForgettingCurve(int adjustment) {
    entity.setForgettingCurveIndex(entity.getForgettingCurveIndex() + adjustment);
    entity.setNextReviewAt(entity.calculateNextReviewAt());
    this.modelFactoryService.save(entity);
  }
}
