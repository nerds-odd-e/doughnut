package com.odde.donut.entities.repositories;

import com.odde.donut.entities.Answer;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.services.RecallAnswerRow;
import jakarta.persistence.LockModeType;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RecallPromptRepository extends CrudRepository<RecallPrompt, Integer> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT prompt FROM RecallPrompt prompt WHERE prompt.id = :id")
  Optional<RecallPrompt> findByIdForUpdate(@Param("id") Integer id);

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

  @Query(
      "SELECT DISTINCT prompt.mcq.id FROM RecallPrompt prompt "
          + "WHERE prompt.memoryTracker.id IN :memoryTrackerIds AND prompt.mcq IS NOT NULL")
  List<Integer> findQuestionIdsByMemoryTrackerIds(
      @Param("memoryTrackerIds") List<Integer> memoryTrackerIds);

  @Query(
      value =
          "SELECT DISTINCT rp.mcq_id"
              + unansweredByMemoryTrackerFrom
              + " AND rp.mcq_id IS NOT NULL",
      nativeQuery = true)
  List<Integer> findUnansweredQuestionIdsByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  List<RecallPrompt> findAllByMemoryTracker_IdOrderByIdDesc(Integer memoryTrackerId);

  List<RecallPrompt> findAllByAnswerIn(List<Answer> answers);

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
  // answer/mcq/memoryTracker associations. Correctness is derived from answer outcome / linked log.
  @Query(
      "SELECT new com.odde.donut.services.RecallAnswerRow("
          + "a.createdAt, a.outcome, rl.grade, a.thinkingTimeMs, rp.createdAt, "
          + "mt.id, rl.retrievability, rp.questionType, a.excludedAt) "
          + "FROM RecallPrompt rp JOIN rp.answer a JOIN rp.memoryTracker mt "
          + "LEFT JOIN RecallLog rl ON rl.answer = a AND rl.memoryTracker = mt "
          + "AND rl.grade IS NOT NULL "
          + "WHERE mt.user.id = :userId AND a.createdAt >= :startTime AND a.createdAt < :endTime "
          + "ORDER BY a.createdAt ASC")
  List<RecallAnswerRow> findAnsweredRecallAnswerRows(
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

  /**
   * Answered MCQ choice indexes for one learner, newest first per attempt (answer time, then answer
   * ID). Prefer {@link #findLatestAnsweredChoiceIndexByMcqIdForUser(Integer, List)}.
   */
  @Query(
      """
      SELECT rp.mcq.id, a.choiceIndex FROM RecallPrompt rp
      JOIN rp.answer a JOIN rp.memoryTracker mt
      WHERE mt.user.id = :userId AND rp.mcq.id IN :mcqIds AND a.choiceIndex IS NOT NULL
      ORDER BY a.createdAt DESC, a.id DESC
      """)
  List<Object[]> findAnsweredChoiceIndexesForUserAndMcqs(
      @Param("userId") Integer userId, @Param("mcqIds") List<Integer> mcqIds);

  /**
   * Latest answered choice index per MCQ for one learner (first row wins after newest-first order).
   */
  default Map<Integer, Integer> findLatestAnsweredChoiceIndexByMcqIdForUser(
      Integer userId, List<Integer> mcqIds) {
    if (mcqIds.isEmpty()) {
      return Map.of();
    }
    Map<Integer, Integer> latest = new HashMap<>();
    for (Object[] row : findAnsweredChoiceIndexesForUserAndMcqs(userId, mcqIds)) {
      latest.putIfAbsent((Integer) row[0], (Integer) row[1]);
    }
    return latest;
  }
}
