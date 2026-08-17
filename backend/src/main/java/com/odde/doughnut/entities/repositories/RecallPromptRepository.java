package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.RecallPrompt;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RecallPromptRepository extends CrudRepository<RecallPrompt, Integer> {

  String unansweredByMemoryTrackerFrom =
      " FROM recall_prompt rp "
          + "LEFT JOIN mcq ON rp.mcq_id = mcq.id "
          + "WHERE rp.memory_tracker_id = :memoryTrackerId "
          + "AND rp.answer_id IS NULL "
          + "AND (mcq.id IS NULL OR mcq.is_contested = false)";

  @Query(
      value = "SELECT rp.*" + unansweredByMemoryTrackerFrom + " ORDER BY rp.id DESC LIMIT 1",
      nativeQuery = true)
  Optional<RecallPrompt> findUnansweredByMemoryTracker(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  @Query(value = "SELECT rp.*" + unansweredByMemoryTrackerFrom, nativeQuery = true)
  List<RecallPrompt> findAllUnansweredByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  List<RecallPrompt> findAllByMemoryTracker_IdOrderByIdDesc(Integer memoryTrackerId);

  @Query(
      value =
          "SELECT rp.* FROM recall_prompt rp "
              + "JOIN answer a ON rp.answer_id = a.id "
              + "JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
              + "WHERE mt.user_id = :userId "
              + "AND a.created_at >= :startTime "
              + "AND a.created_at < :endTime "
              + "ORDER BY a.created_at ASC",
      nativeQuery = true)
  List<RecallPrompt> findAnsweredRecallPromptsInTimeRange(
      @Param("userId") Integer userId,
      @Param("startTime") Timestamp startTime,
      @Param("endTime") Timestamp endTime);

  // Projection (no entity hydration) so the stats endpoint does not N+1 on RecallPrompt's eager
  // answer/mcq/memoryTracker associations. Returns only the 4 fields the aggregator
  // needs.
  @Query(
      "SELECT new com.odde.doughnut.services.RecallAnswerRow("
          + "a.createdAt, a.correct, a.thinkingTimeMs, rp.createdAt) "
          + "FROM RecallPrompt rp JOIN rp.answer a JOIN rp.memoryTracker mt "
          + "WHERE mt.user.id = :userId AND a.createdAt >= :startTime AND a.createdAt < :endTime "
          + "ORDER BY a.createdAt ASC")
  List<com.odde.doughnut.services.RecallAnswerRow> findAnsweredRecallAnswerRows(
      @Param("userId") Integer userId,
      @Param("startTime") Timestamp startTime,
      @Param("endTime") Timestamp endTime);

  @Query(
      value =
          "SELECT DISTINCT mt.user_id FROM recall_prompt rp "
              + "JOIN answer a ON rp.answer_id = a.id "
              + "JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
              + "WHERE a.created_at >= :startTime "
              + "AND a.created_at < :endTime",
      nativeQuery = true)
  List<Integer> findUserIdsWithAnsweredRecallsInTimeRange(
      @Param("startTime") Timestamp startTime, @Param("endTime") Timestamp endTime);

  void deleteByMemoryTracker_Id(Integer memoryTrackerId);
}
