package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.RecallPrompt;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RecallPromptRepository extends CrudRepository<RecallPrompt, Integer> {

  @Query(
      value =
          "SELECT rp.* FROM recall_prompt rp "
              + "LEFT JOIN predefined_question pq ON rp.predefined_question_id = pq.id "
              + "WHERE rp.memory_tracker_id = :memoryTrackerId "
              + "AND rp.quiz_answer_id IS NULL "
              + "AND (pq.id IS NULL OR pq.is_contested = false) "
              + "ORDER BY rp.id DESC LIMIT 1",
      nativeQuery = true)
  Optional<RecallPrompt> findUnansweredByMemoryTracker(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  @Query(
      value =
          "SELECT rp.* FROM recall_prompt rp "
              + "LEFT JOIN predefined_question pq ON rp.predefined_question_id = pq.id "
              + "WHERE rp.memory_tracker_id = :memoryTrackerId "
              + "AND rp.quiz_answer_id IS NULL "
              + "AND (pq.id IS NULL OR pq.is_contested = false)",
      nativeQuery = true)
  List<RecallPrompt> findAllUnansweredByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  List<RecallPrompt> findAllByMemoryTracker_IdOrderByIdDesc(Integer memoryTrackerId);

  @Query(
      value =
          "SELECT rp.* FROM recall_prompt rp "
              + "JOIN quiz_answer qa ON rp.quiz_answer_id = qa.id "
              + "JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
              + "WHERE mt.user_id = :userId "
              + "AND qa.created_at >= :startTime "
              + "AND qa.created_at < :endTime "
              + "ORDER BY qa.created_at ASC",
      nativeQuery = true)
  List<RecallPrompt> findAnsweredRecallPromptsInTimeRange(
      @Param("userId") Integer userId,
      @Param("startTime") Timestamp startTime,
      @Param("endTime") Timestamp endTime);

  // Projection (no entity hydration) so the stats endpoint does not N+1 on RecallPrompt's eager
  // answer/predefinedQuestion/memoryTracker associations. Returns only the 4 fields the aggregator
  // needs.
  @Query(
      "SELECT new com.odde.doughnut.services.RecallAnswerRow("
          + "qa.createdAt, qa.correct, qa.thinkingTimeMs, rp.createdAt) "
          + "FROM RecallPrompt rp JOIN rp.answer qa JOIN rp.memoryTracker mt "
          + "WHERE mt.user.id = :userId AND qa.createdAt >= :startTime AND qa.createdAt < :endTime "
          + "ORDER BY qa.createdAt ASC")
  List<com.odde.doughnut.services.RecallAnswerRow> findAnsweredRecallAnswerRows(
      @Param("userId") Integer userId,
      @Param("startTime") Timestamp startTime,
      @Param("endTime") Timestamp endTime);

  @Query(
      value =
          "SELECT DISTINCT mt.user_id FROM recall_prompt rp "
              + "JOIN quiz_answer qa ON rp.quiz_answer_id = qa.id "
              + "JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
              + "WHERE qa.created_at >= :startTime "
              + "AND qa.created_at < :endTime",
      nativeQuery = true)
  List<Integer> findUserIdsWithAnsweredRecallsInTimeRange(
      @Param("startTime") Timestamp startTime, @Param("endTime") Timestamp endTime);

  @Query(
      value =
          "SELECT COUNT(*) FROM recall_prompt rp "
              + "JOIN quiz_answer qa ON rp.quiz_answer_id = qa.id "
              + "WHERE rp.memory_tracker_id = :memoryTrackerId "
              + "AND qa.correct = false "
              + "AND qa.created_at >= :since",
      nativeQuery = true)
  int countWrongAnswersSinceForMemoryTracker(
      @Param("memoryTrackerId") Integer memoryTrackerId, @Param("since") Timestamp since);

  void deleteByMemoryTracker_Id(Integer memoryTrackerId);
}
