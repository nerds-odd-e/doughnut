package com.odde.donut.controllers.dto;

import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** The learner's whole recall history; every timestamp is ISO-8601 in {@code timezone}. */
public record RecallHistoryExport(
    @NotNull String timezone, @NotNull String exportedAt, @NotNull List<Event> recallEvents) {

  public enum EventKind {
    ANSWERED_RECALL_PROMPT,
    JUST_REVIEW,
    TUTOR_FEEDBACK
  }

  public record Event(
      @NotNull String recordedAt,
      @NotNull @Schema(allowableValues = {"AGAIN", "HARD", "GOOD", "EASY", "CONFUSION"})
          String grade,
      @NotNull EventKind kind,
      String tutorFeedback,
      @NotNull Integer elapsedHours,
      Float stabilityBefore,
      Float difficultyBefore,
      Double retrievability,
      @NotNull Integer memoryTrackerId,
      @NotNull MemoryTrackerType memoryTrackerType,
      Integer noteId,
      Answer answer) {}

  public record Answer(
      @NotNull Integer id,
      QuestionType questionType,
      String promptCreatedAt,
      @NotNull String createdAt,
      Integer thinkingTimeMs,
      Integer awayMs,
      Integer awayCount,
      Integer detourMs,
      Integer detourCount,
      Integer idleMs,
      AnswerOutcome outcome,
      String excludedAt,
      String exclusionReason) {}
}
