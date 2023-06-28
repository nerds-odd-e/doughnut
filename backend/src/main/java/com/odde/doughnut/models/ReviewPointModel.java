package com.odde.doughnut.models;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import com.odde.doughnut.services.AiAdvisorService;
import java.sql.Timestamp;

public record ReviewPointModel(ReviewPoint entity, ModelFactoryService modelFactoryService) {
  public QuizQuestion getRandomQuizQuestion(
      Randomizer randomizer, User user, AiAdvisorService aiAdvisorService) {
    return QuizQuestion.create(
        generateAQuizQuestion(randomizer, user, aiAdvisorService), modelFactoryService, user);
  }

  public ReviewPoint getEntity() {
    return entity;
  }

  public void initialReview(Timestamp currentUTCTimestamp, User user) {
    entity.setUser(user);
    entity.setInitialReviewedAt(currentUTCTimestamp);
    entity.setLastReviewedAt(currentUTCTimestamp);
    updateForgettingCurve(0);
  }

  public QuizQuestionEntity generateAQuizQuestion(
      Randomizer randomizer, User user, AiAdvisorService aiAdvisorService) {
    QuizQuestionDirector quizQuestionDirector =
        new QuizQuestionDirector(entity, randomizer, modelFactoryService, aiAdvisorService);
    return quizQuestionDirector.buildRandomQuestion(user.getAiQuestionTypeOnlyForReview());
  }

  public void updateAfterRepetition(Timestamp currentUTCTimestamp, boolean successful) {
    entity.setRepetitionCount(entity.getRepetitionCount() + 1);
    if (successful) {
      entity.reviewedSuccessfully(currentUTCTimestamp);
    } else {
      entity.reviewFailed(currentUTCTimestamp);
    }
    this.modelFactoryService.reviewPointRepository.save(entity);
  }

  public void updateForgettingCurve(int adjustment) {
    entity.setForgettingCurveIndex(entity.getForgettingCurveIndex() + adjustment);
    entity.setNextReviewAt(entity.calculateNextReviewAt());
    this.modelFactoryService.reviewPointRepository.save(entity);
  }
}
