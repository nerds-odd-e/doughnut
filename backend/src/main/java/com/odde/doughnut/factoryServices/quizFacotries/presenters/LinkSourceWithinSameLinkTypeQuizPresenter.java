package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
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
  protected List<QuizQuestion.Choice> getOptionsFromNote(Stream<Note> noteStream) {
    return noteStream
        .map(
            note -> {
              QuizQuestion.Choice choice = new QuizQuestion.Choice();
              Note source = note.getParent();
              Note target = note.getTargetNote();
              choice.setDisplay(
                  ClozedString.htmlClozedString(source.getTopicConstructor())
                      .hide(target.getNoteTitle())
                      .clozeTitle());
              return choice;
            })
        .toList();
  }
}
