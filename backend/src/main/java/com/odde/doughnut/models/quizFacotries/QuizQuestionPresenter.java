package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.Optional;

public interface QuizQuestionPresenter {
  String instruction();

  String mainTopic();

  boolean isAnswerCorrect(Answer answer);

  default LinksOfANote hintLinks() {
    return null;
  }

  default Optional<PictureWithMask> pictureWithMask() {
    return Optional.empty();
  }

  default List<QuizQuestion.Option> getOptions(
      QuizQuestionEntity quizQuestionEntity, ModelFactoryService modelFactoryService) {
    return List.of();
  }
}
