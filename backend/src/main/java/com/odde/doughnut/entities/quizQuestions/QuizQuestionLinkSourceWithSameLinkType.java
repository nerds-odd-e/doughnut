package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.LinkSourceWithinSameLinkTypeQuizPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.QuizQuestionWithOptionsPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("14")
public class QuizQuestionLinkSourceWithSameLinkType extends QuizQuestionWithNoteChoices {

  @JsonIgnore
  public QuizQuestionWithOptionsPresenter buildPresenter() {
    return new LinkSourceWithinSameLinkTypeQuizPresenter(this);
  }

  @Override
  public String getMainTopic() {
    return getNote().getTargetNote().getTopicConstructor();
  }

  @Override
  public String getStem() {
    return "Which one <em>is immediately " + getNote().getLinkType().label + "</em>:";
  }

  @Override
  protected QuizQuestion.Choice noteToChoice(Note note) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    Note source = note.getParent();
    Note target = note.getTargetNote();
    choice.setDisplay(
        ClozedString.htmlClozedString(source.getTopicConstructor())
            .hide(target.getNoteTitle())
            .clozeTitle());
    return choice;
  }
}
