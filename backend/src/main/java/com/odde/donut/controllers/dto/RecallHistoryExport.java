package com.odde.donut.controllers.dto;

import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * The learner's whole recall history and daily probes; every timestamp is ISO-8601 in {@code
 * timezone}. Donut stores no timezone per event or probe, so this one zone applies to the whole
 * history: on days the learner was elsewhere, the local day and time of day can be wrong.
 */
public record RecallHistoryExport(
    @NotNull String timezone,
    @NotNull String exportedAt,
    @NotNull List<Event> recallEvents,
    @NotNull List<DailyProbe> dailyProbes) {

  public enum EventKind {
    ANSWERED_RECALL_PROMPT,
    JUST_REVIEW,
    TUTOR_FEEDBACK
  }

  @Schema(name = "RecallHistoryEvent")
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

  @Schema(name = "RecallHistoryAnswer")
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

  @Schema(name = "RecallHistoryDailyProbe")
  public record DailyProbe(
      @NotNull String completedAt,
      Double speed,
      @NotNull Integer accuracy,
      @NotNull Integer lapseCount,
      Double variability,
      @NotNull List<DailyProbeTrialDTO> trials) {}
}
