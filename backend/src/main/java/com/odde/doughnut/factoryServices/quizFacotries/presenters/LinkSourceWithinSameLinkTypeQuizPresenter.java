package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import java.util.List;
import java.util.stream.Stream;

public class LinkSourceWithinSameLinkTypeQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Note link;

  public LinkSourceWithinSameLinkTypeQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getNote();
  }

  @Override
  public String mainTopic() {
    return link.getTargetNote().getTopicConstructor();
  }

  @Override
  public String stem() {
    return "Which one <em>is immediately " + link.getLinkType().label + "</em>:";
  }

  @Override
  protected List<QuizQuestion.Choice> getOptionsFromThings(Stream<Thing> noteStream) {
    return noteStream
        .map(
            thing -> {
              QuizQuestion.Choice choice = new QuizQuestion.Choice();
              choice.setDisplay(thing.getClozeSource().clozeTitle());
              return choice;
            })
        .toList();
  }
}
