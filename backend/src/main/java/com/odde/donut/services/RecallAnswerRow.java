package com.odde.donut.services;

import com.odde.donut.entities.Answer;
import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.Grade;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Minimal projection of an answered recall prompt — the only fields the stats aggregation needs.
 * Fetched via a JPQL constructor expression so Hibernate never hydrates {@code RecallPrompt}
 * entities or their eager associations (avoids the N+1 that caused the endpoint timeout).
 */
public record RecallAnswerRow(
    Timestamp answerCreatedAt,
    AnswerOutcome answerOutcome,
    Grade grade,
    Integer thinkingTimeMs,
    Timestamp promptCreatedAt,
    Integer memoryTrackerId) {

  boolean correct() {
    return Boolean.TRUE.equals(Answer.correctFrom(answerOutcome, grade));
  }

  boolean countsAsReview() {
    return answerOutcome != AnswerOutcome.OVERLAP;
  }

  /**
   * Raw elapsed time for this answer, uncapped: the {@code thinkingTimeMs} instrumentation reading,
   * falling back to the prompt/answer timestamp diff only when {@code thinkingTimeMs} is null.
   * Callers apply their own floors/caps on top of this value — see {@link RecallPaceAggregator}
   * (on-task time for pace/retention, uncapped) and {@link RecallStatsAggregator#responseTimeMs}
   * (response time for the trend/AM-PM charts, capped).
   */
  Optional<Long> rawElapsedMs() {
    if (answerCreatedAt == null) {
      return Optional.empty();
    }
    if (thinkingTimeMs != null) {
      return Optional.of((long) thinkingTimeMs);
    }
    if (promptCreatedAt == null) {
      return Optional.empty();
    }
    return Optional.of(answerCreatedAt.getTime() - promptCreatedAt.getTime());
  }
}
