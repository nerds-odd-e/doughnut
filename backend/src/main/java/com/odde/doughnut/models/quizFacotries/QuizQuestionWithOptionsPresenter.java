package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.stream.Stream;

public abstract class QuizQuestionWithOptionsPresenter implements QuizQuestionPresenter {
  public QuizQuestion.OptionCreator optionCreator() {
    return new QuizQuestion.TitleOptionCreator();
  }

  @Override
  public List<QuizQuestion.Option> getOptions(
      QuizQuestionEntity quizQuestionEntity, ModelFactoryService modelFactoryService) {
    QuizQuestion.OptionCreator optionCreator = optionCreator();
    Stream<Thing> noteStream =
        modelFactoryService.getThingStreamAndKeepOriginalOrder(
            quizQuestionEntity.getOptionThingIds());
    return noteStream.map(optionCreator::optionFromThing).toList();
  }
}
