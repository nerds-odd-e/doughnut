package com.odde.doughnut.models;

import static com.odde.doughnut.entities.SelfEvaluate.satisfying;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.entities.SelfEvaluate;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import java.sql.Timestamp;
import java.util.Optional;

public record ReviewPointModel(ReviewPoint entity, ModelFactoryService modelFactoryService) {
  public ReviewPoint getEntity() {
    return entity;
  }

  public void initialReview(
      UserModel userModel, ReviewSetting reviewSetting, Timestamp currentUTCTimestamp) {
    Note note = entity.getNote();
    if (note != null) {
      note.mergeMasterReviewSetting(reviewSetting);
      modelFactoryService.noteRepository.save(note);
    }
    entity.setUser(userModel.getEntity());
    entity.setInitialReviewedAt(currentUTCTimestamp);
    evaluate(currentUTCTimestamp, satisfying);
  }

  private void updateNextRepetitionWithAdjustment(Timestamp currentUTCTimestamp, int adjustment) {
    entity.changeNextRepetitionWithAdjustment(currentUTCTimestamp, adjustment);
    this.modelFactoryService.reviewPointRepository.save(entity);
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
        .orElse(entity.createAQuizQuestionOfType(QuestionType.JUST_REVIEW));
  }

  public void updateReviewPoint(Timestamp currentUTCTimestamp, SelfEvaluate selfEvaluate) {
    increaseRepetitionCountAndSave();
    evaluate(currentUTCTimestamp, selfEvaluate);
  }

  public void evaluate(Timestamp currentUTCTimestamp, SelfEvaluate selfEvaluation) {
    updateNextRepetitionWithAdjustment(currentUTCTimestamp, selfEvaluation.adjustment);
  }
}
