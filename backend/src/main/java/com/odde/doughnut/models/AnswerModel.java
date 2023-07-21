package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import java.util.stream.Stream;

public class AnswerModel {
  private final Answer answer;
  private final ModelFactoryService modelFactoryService;

  private Boolean cachedResult;

  public AnswerModel(Answer answer, ModelFactoryService modelFactoryService) {
    this.answer = answer;
    this.modelFactoryService = modelFactoryService;
  }

  public void updateReviewPoints(Timestamp currentUTCTimestamp) {
    boolean correct = isCorrect();
    relatedReviewPoints()
        .map(this.modelFactoryService::toReviewPointModel)
        .forEach(model -> model.updateAfterRepetition(currentUTCTimestamp, correct));
  }

  private Stream<ReviewPoint> relatedReviewPoints() {
    Stream<ReviewPoint> reviewPointStream =
        answer.getQuestion().getViceReviewPointIdList().stream()
            .flatMap(
                rPid -> this.modelFactoryService.reviewPointRepository.findById(rPid).stream());
    ReviewPoint reviewPoint = answer.getQuestion().getReviewPoint();
    if (reviewPoint != null) {
      return Stream.concat(reviewPointStream, Stream.of(reviewPoint));
    }
    return reviewPointStream;
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
    cachedResult = answer.getQuestion().isAnswerCorrect(getAnswerDisplay());
    return cachedResult;
  }

  private Note getAnswerNote() {
    if (answer.getAnswerNoteId() == null) return null;
    return this.modelFactoryService.noteRepository.findById(answer.getAnswerNoteId()).orElse(null);
  }
}
