package com.odde.doughnut.services;

import java.sql.Timestamp;

/**
 * Minimal projection of an answered recall prompt — the only fields the stats aggregation needs.
 * Fetched via a JPQL constructor expression so Hibernate never hydrates {@code RecallPrompt}
 * entities or their eager associations (avoids the N+1 that caused the endpoint timeout).
 */
public record RecallAnswerRow(
    Timestamp answerCreatedAt,
    Boolean correct,
    Integer thinkingTimeMs,
    Timestamp promptCreatedAt) {}
