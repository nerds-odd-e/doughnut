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

  @Query(
      value =
          "SELECT COUNT(*) FROM recall_prompt rp "
              + "JOIN quiz_answer qa ON rp.quiz_answer_id = qa.id "
              + "JOIN predefined_question pq ON rp.predefined_question_id = pq.id "
              + "WHERE pq.note_id = :noteId "
              + "AND qa.correct = false "
              + "AND qa.created_at >= :since",
      nativeQuery = true)
  int countWrongAnswersSince(@Param("noteId") Integer noteId, @Param("since") Timestamp since);

  void deleteByMemoryTracker_Id(Integer memoryTrackerId);
}
