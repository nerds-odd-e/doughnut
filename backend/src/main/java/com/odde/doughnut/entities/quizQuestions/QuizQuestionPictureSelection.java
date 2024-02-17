package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.PictureSelectionQuizPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("4")
public class QuizQuestionPictureSelection extends QuizQuestionWithNoteChoices {

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new PictureSelectionQuizPresenter(this);
  }
}
