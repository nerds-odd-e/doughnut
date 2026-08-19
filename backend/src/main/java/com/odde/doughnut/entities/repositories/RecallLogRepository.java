package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.ProductOutcome;
import com.odde.doughnut.entities.RecallLog;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RecallLogRepository extends CrudRepository<RecallLog, Integer> {
  List<RecallLog> findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(Integer memoryTrackerId);

  @Query(
      """
      SELECT new com.odde.doughnut.entities.repositories.TutorLogSummary(
        COUNT(rl), MAX(rl.recordedAt))
      FROM RecallLog rl
      WHERE rl.memoryTracker.id = :memoryTrackerId
        AND rl.answer IS NULL
        AND rl.productOutcome <> com.odde.doughnut.entities.ProductOutcome.CONFUSION
      """)
  TutorLogSummary summarizeTutorLogsByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  @Query(
      """
      SELECT rl.productOutcome
      FROM RecallLog rl
      WHERE rl.memoryTracker.id = :memoryTrackerId
        AND rl.answer IS NULL
        AND rl.productOutcome <> com.odde.doughnut.entities.ProductOutcome.CONFUSION
      ORDER BY rl.recordedAt DESC, rl.id DESC
      LIMIT 1
      """)
  Optional<ProductOutcome> findLatestTutorProductOutcomeByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  @Query(
      """
      SELECT COUNT(rl)
      FROM RecallLog rl
      WHERE rl.memoryTracker.id = :memoryTrackerId
        AND rl.productOutcome = com.odde.doughnut.entities.ProductOutcome.AGAIN
        AND rl.recordedAt >= :since
      """)
  int countAgainOutcomesSinceForMemoryTracker(
      @Param("memoryTrackerId") Integer memoryTrackerId, @Param("since") Timestamp since);
}
