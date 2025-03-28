package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RecallPromptRepository extends CrudRepository<RecallPrompt, Integer> {

  @Query(
      "SELECT rp FROM RecallPrompt rp "
          + "JOIN rp.predefinedQuestion pq "
          + "WHERE pq.note = :note "
          + "AND rp.answer IS NULL "
          + "AND pq.contested = false")
  List<RecallPrompt> findUnansweredByNote(@Param("note") Note note);
}
