package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.stream.Stream;

public abstract class QuizQuestionWithOptionsPresenter implements QuizQuestionPresenter {

  protected final QuizQuestionEntity quizQuestion;

  public QuizQuestionWithOptionsPresenter(QuizQuestionEntity quizQuestion) {
    this.quizQuestion = quizQuestion;
  }

  @Override
  public List<QuizQuestion.Option> getOptions(ModelFactoryService modelFactoryService) {
    Stream<Thing> thingStream =
        modelFactoryService.getThingStreamAndKeepOriginalOrder(quizQuestion.getChoiceThingIds());
    return getOptionsFromThings(thingStream);
  }

  protected List<QuizQuestion.Option> getOptionsFromThings(Stream<Thing> noteStream) {
    return noteStream
        .map(
            thing -> {
              QuizQuestion.Option option = new QuizQuestion.Option();
              option.setNoteId(thing.getNote().getId());
              option.setDisplay(thing.getNote().getTitle());
              return option;
            })
        .toList();
  }
}
