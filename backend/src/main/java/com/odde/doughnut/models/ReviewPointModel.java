package com.odde.doughnut.models;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import com.odde.doughnut.services.AiAdvisorService;

import java.sql.Timestamp;
import java.util.Optional;

public record ReviewPointModel(ReviewPoint entity, ModelFactoryService modelFactoryService) {
  public QuizQuestionViewedByUser getRandomQuizQuestion(Randomizer randomizer, User user, AiAdvisorService aiAdvisorService) {
    return QuizQuestionViewedByUser.create(
        generateAQuizQuestion(randomizer, user, aiAdvisorService), modelFactoryService, user );
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

  public QuizQuestion generateAQuizQuestion(Randomizer randomizer, User user, AiAdvisorService aiAdvisorService) {
    return randomizer.shuffle(entity.availableQuestionTypes(user)).stream()
        .map(type -> new QuizQuestionDirector(entity, type, randomizer, modelFactoryService))
        .map(QuizQuestionDirector::buildQuizQuestion)
        .flatMap(Optional::stream)
        .findFirst()
        .orElseGet(() -> entity.createAQuizQuestionOfType(QuizQuestion.QuestionType.JUST_REVIEW));
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
