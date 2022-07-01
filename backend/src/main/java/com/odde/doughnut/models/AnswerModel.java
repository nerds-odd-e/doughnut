package com.odde.doughnut.models;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.AnswerViewedByUser;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public class AnswerModel {
  private final Answer answer;
  private final ModelFactoryService modelFactoryService;

  private Boolean cachedResult;

  public AnswerModel(Answer answer, ModelFactoryService modelFactoryService) {
    this.answer = answer;
    this.modelFactoryService = modelFactoryService;
  }

  public void updateReviewPoints(Timestamp currentUTCTimestamp) {
    answer
        .getQuestion()
        .getViceReviewPointIdList()
        .forEach(
            rPid ->
                this.modelFactoryService
                    .reviewPointRepository
                    .findById(rPid)
                    .ifPresent(
                        vice ->
                            this.modelFactoryService
                                .toReviewPointModel(vice)
                                .updateAfterRepetition(currentUTCTimestamp, isCorrect())));
    ReviewPointModel reviewPointModel =
        this.modelFactoryService.toReviewPointModel(answer.getQuestion().getReviewPoint());
    reviewPointModel.updateAfterRepetition(currentUTCTimestamp, isCorrect());
  }

  public AnswerViewedByUser getAnswerViewedByUser() {
    AnswerViewedByUser answerResult = new AnswerViewedByUser();
    answerResult.answerId = answer.getId();
    answerResult.correct = isCorrect();
    answerResult.answerDisplay = getAnswerDisplay();
    return answerResult;
  }

  public AnswerResult getAnswerResult() {
    AnswerResult answerResult = new AnswerResult();
    answerResult.answerId = answer.getId();
    answerResult.correct = isCorrect();
    return answerResult;
  }

  public void save() {
    modelFactoryService.answerRepository.save(answer);
  }

  private String getAnswerDisplay() {
    if (getAnswerNote() != null) {
      return getAnswerNote().getTitle();
    }
    return answer.getSpellingAnswer();
  }

  private boolean isCorrect() {
    if (cachedResult != null) return cachedResult;
    cachedResult =
        answer.getQuestion().isAnswerCorrect(getAnswerNote(), answer.getSpellingAnswer());
    return cachedResult;
  }

  private Note getAnswerNote() {
    if (answer.getAnswerNoteId() == null) return null;
    return this.modelFactoryService.noteRepository.findById(answer.getAnswerNoteId()).orElse(null);
  }
}
