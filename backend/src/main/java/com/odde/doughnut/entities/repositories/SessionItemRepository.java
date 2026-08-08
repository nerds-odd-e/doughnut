package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.SessionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionItemRepository extends JpaRepository<SessionItem, Integer> {

  List<SessionItem> findByLearningSession_Id(Integer learningSessionId);

  void deleteByLearningSession_Id(Integer learningSessionId);

  @Query(
      """
      SELECT new com.odde.doughnut.entities.repositories.RecordedFeedbackSummary(
        COUNT(si), MAX(si.feedbackRecordedAt))
      FROM SessionItem si
      WHERE si.memoryTracker.id = :memoryTrackerId
      AND si.feedbackScore IS NOT NULL
      AND si.learningSession.status = com.odde.doughnut.entities.LearningSessionStatus.RECORDED
      """)
  RecordedFeedbackSummary summarizeRecordedFeedbackByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  @Query(
      """
      SELECT si.memoryTracker.id
      FROM SessionItem si
      WHERE si.learningSession.user.id = :userId
      AND si.learningSession.status = com.odde.doughnut.entities.LearningSessionStatus.AWAITING_REPORT
      """)
  List<Integer> findMemoryTrackerIdsInAwaitingReportSessions(@Param("userId") Integer userId);

  @Query(
      """
      SELECT si.feedbackScore
      FROM SessionItem si
      WHERE si.memoryTracker.id = :memoryTrackerId
      AND si.feedbackScore IS NOT NULL
      AND si.learningSession.status = com.odde.doughnut.entities.LearningSessionStatus.RECORDED
      AND si.feedbackRecordedAt = (
        SELECT MAX(si2.feedbackRecordedAt)
        FROM SessionItem si2
        WHERE si2.memoryTracker.id = :memoryTrackerId
        AND si2.feedbackScore IS NOT NULL
        AND si2.learningSession.status = com.odde.doughnut.entities.LearningSessionStatus.RECORDED
      )
      """)
  java.util.Optional<Integer> findLatestFeedbackScoreByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);
}
