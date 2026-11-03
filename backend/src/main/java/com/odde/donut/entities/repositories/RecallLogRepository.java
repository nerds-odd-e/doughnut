package com.odde.donut.entities.repositories;

import com.odde.donut.entities.RecallLog;
import com.odde.donut.services.RecallEventRow;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RecallLogRepository extends CrudRepository<RecallLog, Integer> {
  List<RecallLog> findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(Integer memoryTrackerId);

  List<RecallLog> findAllByMemoryTracker_IdOrderByRecordedAtAscIdAsc(Integer memoryTrackerId);

  @Query(
      """
      SELECT rl
      FROM RecallLog rl
      WHERE rl.memoryTracker.id = :memoryTrackerId
        AND rl.answer IS NULL
        AND rl.grade IS NOT NULL
      ORDER BY rl.recordedAt DESC, rl.id DESC
      """)
  List<RecallLog> findTutorLogsByMemoryTrackerId(@Param("memoryTrackerId") Integer memoryTrackerId);

  @Query(
      """
      SELECT COUNT(rl)
      FROM RecallLog rl LEFT JOIN rl.answer a
      WHERE rl.memoryTracker.id = :memoryTrackerId
        AND rl.grade = com.odde.donut.entities.Grade.AGAIN
        AND a.excludedAt IS NULL
        AND rl.recordedAt >= :since
      """)
  int countAgainOutcomesSinceForMemoryTracker(
      @Param("memoryTrackerId") Integer memoryTrackerId, @Param("since") Timestamp since);

  // Projection (no entity hydration): one row per recall log, oldest first, with the answer and
  // the prompt that owns it (a CONFUSION log shares the answer of a prompt on another tracker).
  @Query(
      """
      SELECT new com.odde.donut.services.RecallEventRow(
        rl.recordedAt, rl.grade, rl.tutorFeedback, rl.elapsedHours, rl.stabilityBefore,
        rl.difficultyBefore, rl.retrievability, mt.id, mt.type, mt.note.id, rp.questionType,
        rp.createdAt, a.id, a.createdAt, a.thinkingTimeMs, a.awayMs, a.awayCount, a.detourMs,
        a.detourCount, a.idleMs, a.outcome, a.excludedAt, a.exclusionReason)
      FROM RecallLog rl JOIN rl.memoryTracker mt
      LEFT JOIN rl.answer a LEFT JOIN RecallPrompt rp ON rp.answer = a
      WHERE mt.user.id = :userId
      ORDER BY rl.recordedAt ASC, rl.id ASC
      """)
  List<RecallEventRow> findRecallEventRows(@Param("userId") Integer userId);
}
