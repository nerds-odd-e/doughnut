package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.EntityWithId;
import java.util.ArrayList;
import java.util.List;

public interface QuestionOptionsFactory {
  EntityWithId generateAnswer();

  List<? extends EntityWithId> generateFillingOptions();

  default List<EntityWithId> getOptionEntities() {
    EntityWithId answerNote = generateAnswer();
    if (answerNote == null) return List.of();
    List<EntityWithId> options = new ArrayList<>(generateFillingOptions());
    options.add(answerNote);
    return options;
  }
}
