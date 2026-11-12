package com.odde.donut.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** One answer as recorded: when, whether it was correct, the question, and its source note. */
public record RecallAttempt(
    Instant answeredAt, Boolean correct, SourceNote note, MultipleChoice multipleChoice) {

  public record SourceNote(String title, String portablePath) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record MultipleChoice(
      String stem,
      List<String> choices,
      String selectedChoice,
      String expectedAnswer,
      String validationRationale) {}
}
