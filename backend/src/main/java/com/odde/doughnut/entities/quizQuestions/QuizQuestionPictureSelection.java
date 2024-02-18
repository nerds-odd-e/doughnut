package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.PictureSelectionQuizPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.QuizQuestionWithOptionsPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("4")
public class QuizQuestionPictureSelection extends QuizQuestionWithNoteChoices {

  @JsonIgnore
  public QuizQuestionWithOptionsPresenter buildPresenter() {
    return new PictureSelectionQuizPresenter(this);
  }

  @Override
  public String getMainTopic() {
    return getNote().getTopicConstructor();
  }

  @Override
  protected QuizQuestion.Choice noteToChoice(Note note) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    choice.setDisplay(note.getTopicConstructor());
    choice.setPictureWithMask(note.getPictureWithMask().orElse(null));
    choice.setPicture(true);
    return choice;
  }
}
