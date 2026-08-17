package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.SessionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionItemRepository extends JpaRepository<SessionItem, Integer> {

  List<SessionItem> findByLearningSession_Id(Integer learningSessionId);

  @Query(
      """
      SELECT si.feedbackScore
      FROM SessionItem si
      WHERE si.memoryTracker.id = :memoryTrackerId
      AND si.feedbackRecordedAt = (
        SELECT MAX(si2.feedbackRecordedAt)
        FROM SessionItem si2
        WHERE si2.memoryTracker.id = :memoryTrackerId
      )
      """)
  java.util.Optional<Integer> findLatestFeedbackScoreByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);
}
