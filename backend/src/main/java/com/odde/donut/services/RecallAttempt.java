package com.odde.donut.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.services.focusContext.FocusContextResult;
import java.time.Instant;
import java.util.List;

/**
 * One answer as recorded: when, whether it was correct, any special spelling outcome, any ambiguity
 * exclusion, the question, and its source note.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecallAttempt(
    Instant answeredAt,
    Boolean correct,
    AnswerOutcome outcome,
    AmbiguityExclusion excludedAsAmbiguous,
    SourceNote note,
    MultipleChoice multipleChoice,
    Spelling spelling) {

  /** The learner excluded the attempt as ambiguous; its recorded correctness is kept. */
  public record AmbiguityExclusion(String reason) {}

  /**
   * The note being recalled; a missed spelling adds its full current content, a missed multiple
   * choice its bounded graph.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record SourceNote(
      String title, String portablePath, String content, FocusContextResult graph) {}

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
