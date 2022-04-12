package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    evaluate(currentUTCTimestamp, "satisfying");
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
    return new QuizQuestionGenerator(entity, randomizer, modelFactoryService).generateQuestion();
  }

  public void updateReviewPoint(Timestamp currentUTCTimestamp, String selfEvaluate) {
    increaseRepetitionCountAndSave();
    evaluate(currentUTCTimestamp, selfEvaluate);
  }

  public void evaluate(Timestamp currentUTCTimestamp, String selfEvaluation) {
    updateNextRepetitionWithAdjustment(currentUTCTimestamp, getAdjustment(selfEvaluation));
  }

  private int getAdjustment(String selfEvaluation) {
    if ("reset".equals(selfEvaluation)) {
      return -2;
    }
    if ("satisfying".equals(selfEvaluation)) {
      return 0;
    }
    if ("sad".equals(selfEvaluation)) {
      return -1;
    }
    if ("happy".equals(selfEvaluation)) {
      return 1;
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  }
}
