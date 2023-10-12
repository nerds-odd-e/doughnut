package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface QuestionSuggestionForFineTuningRepository
    extends CrudRepository<SuggestedQuestionForFineTuning, Integer> {

  @Query(
      value =
          "select count(1) from suggested_question_for_fine_tuning where quiz_question_id = :id and user_id = :userId",
      nativeQuery = true)
  public Integer countByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);
}
