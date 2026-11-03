package com.odde.donut.services;

import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.QuestionType;
import java.sql.Timestamp;

/**
 * Projection of one recall log with its tracker, answer, and the prompt that owns the answer,
 * fetched by a JPQL constructor expression so the export never hydrates entities per row.
 */
public record RecallEventRow(
    Timestamp recordedAt,
    Grade grade,
    String tutorFeedback,
    Integer elapsedHours,
    Float stabilityBefore,
    Float difficultyBefore,
    Double retrievability,
    Integer memoryTrackerId,
    MemoryTrackerType memoryTrackerType,
    Integer noteId,
    QuestionType questionType,
    Timestamp promptCreatedAt,
    Integer answerId,
    Timestamp answerCreatedAt,
    Integer thinkingTimeMs,
    Integer awayMs,
    Integer awayCount,
    Integer detourMs,
    Integer detourCount,
    Integer idleMs,
    AnswerOutcome outcome,
    Timestamp excludedAt,
    String exclusionReason) {}
