package com.odde.donut.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * The learner's recall attempts answered within {@code from} to {@code to}, newest first; {@code
 * nextCursor} continues the window when more remain.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecallAnswerHistory(
    Instant from, Instant to, List<RecallAttempt> attempts, String nextCursor) {}
