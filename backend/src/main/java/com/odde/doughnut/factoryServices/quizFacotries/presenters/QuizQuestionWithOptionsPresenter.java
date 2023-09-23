package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import java.util.List;
import java.util.stream.Stream;

public abstract class QuizQuestionWithOptionsPresenter implements QuizQuestionPresenter {

  protected final QuizQuestionEntity quizQuestion;

  public QuizQuestionWithOptionsPresenter(QuizQuestionEntity quizQuestion) {
    this.quizQuestion = quizQuestion;
  }

  @Override
  public List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService) {
    Stream<Thing> thingStream =
        modelFactoryService.getThingStreamAndKeepOriginalOrder(quizQuestion.getChoiceThingIds());
    return getOptionsFromThings(thingStream);
  }

  protected List<QuizQuestion.Choice> getOptionsFromThings(Stream<Thing> noteStream) {
    return noteStream
        .map(
            thing -> {
              QuizQuestion.Choice choice = new QuizQuestion.Choice();
              choice.setDisplay(thing.getNote().getTopic());
              return choice;
            })
        .toList();
  }
}
