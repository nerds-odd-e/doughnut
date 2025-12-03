package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.RecallPrompt;
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
              + "WHERE rp.memory_tracker_id = :memoryTrackerId "
              + "AND rp.quiz_answer_id IS NULL",
      nativeQuery = true)
  List<RecallPrompt> findAllUnansweredByMemoryTrackerId(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  List<RecallPrompt> findAllByMemoryTrackerIdOrderByIdDesc(Integer memoryTrackerId);
}
