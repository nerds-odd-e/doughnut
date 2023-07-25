package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.Optional;

public interface QuizQuestionPresenter {
  String stem();

  String mainTopic();

  default Optional<PictureWithMask> pictureWithMask() {
    return Optional.empty();
  }

  default List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService) {
    return List.of();
  }
}
