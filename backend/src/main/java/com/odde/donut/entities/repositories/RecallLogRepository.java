package com.odde.donut.entities.repositories;

import com.odde.donut.entities.RecallLog;
import java.sql.Timestamp;
import java.util.List;
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
      SELECT COUNT(rl)
      FROM RecallLog rl
      WHERE rl.memoryTracker.id = :memoryTrackerId
        AND rl.grade = com.odde.donut.entities.Grade.AGAIN
        AND rl.recordedAt >= :since
      """)
  int countAgainOutcomesSinceForMemoryTracker(
      @Param("memoryTrackerId") Integer memoryTrackerId, @Param("since") Timestamp since);
}
