package com.odde.doughnut.models;

import static com.odde.doughnut.entities.SelfEvaluate.*;

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
    updateNextRepetitionWithAdjustment(currentUTCTimestamp, 0);
  }

  public void increaseRepetitionCountAndSave() {
    final int repetitionCount = entity.getRepetitionCount() + 1;
    entity.setRepetitionCount(repetitionCount);
    this.modelFactoryService.reviewPointRepository.save(entity);
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
    increaseRepetitionCountAndSave();
    updateNextRepetitionWithAdjustment(currentUTCTimestamp, selfEvaluate.adjustment);
  }

  public void updateNextRepetitionWithAdjustment(Timestamp currentUTCTimestamp, int adjustment) {
    long delayInHours =
        TimestampOperations.getDiffInHours(
            currentUTCTimestamp, entity.calculateDefaultNextReviewAt());

    entity.updateForgettingCurve(delayInHours, adjustment);

    if (adjustment < 0) {
      entity.setNextReviewAt(TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, 12));
    } else {
      entity.setNextReviewAt(entity.calculateNextReviewAt(currentUTCTimestamp));
    }
    entity.setLastReviewedAt(currentUTCTimestamp);
    this.modelFactoryService.reviewPointRepository.save(entity);
  }

  public void evaluate(SelfEvaluate selfEvaluation) {
    entity.updateForgettingCurve(0, selfEvaluation.adjustment);
    entity.setNextReviewAt(entity.calculateDefaultNextReviewAt());
    this.modelFactoryService.reviewPointRepository.save(entity);
  }
}
