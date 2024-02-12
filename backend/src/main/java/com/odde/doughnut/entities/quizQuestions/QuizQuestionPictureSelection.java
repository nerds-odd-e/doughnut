package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.PictureSelectionQuizPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("4")
public class QuizQuestionPictureSelection extends QuizQuestionEntity {

  public QuizQuestionPictureSelection(Note note) {
    super(note);
  }

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new PictureSelectionQuizPresenter(this);
  }
}
