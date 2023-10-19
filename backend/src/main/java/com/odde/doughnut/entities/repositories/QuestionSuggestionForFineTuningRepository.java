package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import org.springframework.data.repository.CrudRepository;

public interface QuestionSuggestionForFineTuningRepository
    extends CrudRepository<SuggestedQuestionForFineTuning, Integer> {}
