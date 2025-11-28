package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface QuestionAnswerRepository extends CrudRepository<QuestionAnswer, Integer> {

  @Query(
      "SELECT qa FROM QuestionAnswer qa "
          + "JOIN qa.predefinedQuestion pq "
          + "WHERE pq.note = :note "
          + "AND pq.contested = false")
  List<QuestionAnswer> findByNote(@Param("note") Note note);
}
