package com.odde.doughnut.models;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.SelfEvaluate;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import java.sql.Timestamp;
import java.util.Optional;

public record ReviewPointModel(ReviewPoint entity, ModelFactoryService modelFactoryService) {
  public ReviewPoint getEntity() {
    return entity;
  }

  public void initialReview(UserModel userModel, Timestamp currentUTCTimestamp) {
    entity.setUser(userModel.getEntity());
    entity.setInitialReviewedAt(currentUTCTimestamp);
    entity.setLastReviewedAt(currentUTCTimestamp);
    evaluate(0);
  }

  public QuizQuestion generateAQuizQuestion(Randomizer randomizer) {
    return randomizer.shuffle(entity.availableQuestionTypes()).stream()
        .map(type -> new QuizQuestionDirector(entity, type, randomizer, modelFactoryService))
        .map(QuizQuestionDirector::buildQuizQuestion)
        .flatMap(Optional::stream)
        .findFirst()
        .orElseGet(() -> entity.createAQuizQuestionOfType(QuizQuestion.QuestionType.JUST_REVIEW));
  }

  public void updateAfterRepetition(Timestamp currentUTCTimestamp, SelfEvaluate selfEvaluate) {
    entity.setRepetitionCount(entity.getRepetitionCount() + 1);
    entity.updateNextRepetitionWithAdjustment(currentUTCTimestamp, selfEvaluate.adjustment);
    this.modelFactoryService.reviewPointRepository.save(entity);
  }

  public void evaluate(int adjustment) {
    entity.addToForgettingCurve(adjustment);
    entity.setNextReviewAt(entity.calculateNextReviewAt());
    this.modelFactoryService.reviewPointRepository.save(entity);
  }
}
