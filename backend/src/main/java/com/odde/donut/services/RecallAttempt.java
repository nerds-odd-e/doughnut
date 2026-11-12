package com.odde.donut.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.donut.entities.AnswerOutcome;
import java.time.Instant;
import java.util.List;

/**
 * One answer as recorded: when, whether it was correct, any special spelling outcome, the question,
 * and its source note.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecallAttempt(
    Instant answeredAt,
    Boolean correct,
    AnswerOutcome outcome,
    SourceNote note,
    MultipleChoice multipleChoice,
    Spelling spelling) {

  public record SourceNote(String title, String portablePath) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record MultipleChoice(
      String stem,
      List<String> choices,
      String selectedChoice,
      String expectedAnswer,
      String validationRationale) {}

  /** A spelling prompt is derived from the note, so a later note edit loses what was shown. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Spelling(String typedAnswer, String prompt, String promptUnavailable) {}
}
