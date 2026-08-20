package com.odde.doughnut.services;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerOutcome;
import com.odde.doughnut.entities.Grade;
import java.sql.Timestamp;

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
    Timestamp promptCreatedAt) {

  boolean correct() {
    return Boolean.TRUE.equals(Answer.correctFrom(answerOutcome, grade));
  }

  boolean countsAsReview() {
    return answerOutcome != AnswerOutcome.OVERLAP;
  }
}
