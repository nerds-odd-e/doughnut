package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.Thing;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionSuggestionForFineTuningRepository
    extends CrudRepository<SuggestedQuestionForFineTuning, Integer> {
  @Query("select * from suggested_question_for_fine_tuning where is_positive_feedback = TRUE")
  public List<SuggestedQuestionForFineTuning> getAllFineTuneData();


}


