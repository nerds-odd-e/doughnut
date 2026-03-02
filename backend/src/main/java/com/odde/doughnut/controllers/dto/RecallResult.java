package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.Note;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = RecallResult.QuestionResult.class, name = "QuestionResult"),
  @JsonSubTypes.Type(value = RecallResult.SpellingResult.class, name = "SpellingResult")
})
public sealed interface RecallResult
    permits RecallResult.QuestionResult, RecallResult.SpellingResult {

  record QuestionResult(AnsweredQuestion answeredQuestion) implements RecallResult {}

  record SpellingResult(
      Note note, Answer answer, Integer memoryTrackerId, Boolean thresholdExceeded)
      implements RecallResult {}
}
