package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.Optional;

public interface QuizQuestionPresenter {
  String instruction();

  String mainTopic();

  default boolean isAnswerCorrect(Answer answer) {
    return false;
  }

  default LinksOfANote hintLinks() {
    return null;
  }

  default Optional<PictureWithMask> pictureWithMask() {
    return Optional.empty();
  }

  default List<QuizQuestion.Option> getOptions(ModelFactoryService modelFactoryService) {
    return List.of();
  }
}
