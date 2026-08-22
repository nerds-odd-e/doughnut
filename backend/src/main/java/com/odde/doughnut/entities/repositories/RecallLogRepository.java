package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Grade;
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
      SELECT rl.grade
      FROM RecallLog rl
      WHERE rl.memoryTracker.id = :memoryTrackerId
        AND rl.answer IS NULL
        AND rl.grade IS NOT NULL
      ORDER BY rl.recordedAt DESC, rl.id DESC
      LIMIT 1
      """)
  Optional<Grade> findLatestTutorGradeByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  @Query(
      """
      SELECT COUNT(rl)
      FROM RecallLog rl
      WHERE rl.memoryTracker.id = :memoryTrackerId
        AND rl.grade = com.odde.doughnut.entities.Grade.AGAIN
        AND rl.recordedAt >= :since
      """)
  int countAgainOutcomesSinceForMemoryTracker(
      @Param("memoryTrackerId") Integer memoryTrackerId, @Param("since") Timestamp since);
}
