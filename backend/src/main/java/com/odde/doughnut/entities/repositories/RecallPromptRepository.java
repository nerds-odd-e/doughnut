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
              + "JOIN predefined_question pq ON rp.predefined_question_id = pq.id "
              + "WHERE rp.memory_tracker_id = :memoryTrackerId "
              + "AND rp.quiz_answer_id IS NULL "
              + "AND pq.is_contested = false "
              + "ORDER BY rp.id DESC LIMIT 1",
      nativeQuery = true)
  Optional<RecallPrompt> findUnansweredByMemoryTracker(
      @Param("memoryTrackerId") Integer memoryTrackerId);

  List<RecallPrompt> findAllByMemoryTrackerIdOrderByIdDesc(Integer memoryTrackerId);
}
