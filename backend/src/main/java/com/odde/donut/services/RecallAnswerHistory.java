package com.odde.donut.services;

import java.time.Instant;
import java.util.List;

/** The learner's recall attempts answered within {@code from} to {@code to}, newest first. */
public record RecallAnswerHistory(Instant from, Instant to, List<RecallAttempt> attempts) {}
