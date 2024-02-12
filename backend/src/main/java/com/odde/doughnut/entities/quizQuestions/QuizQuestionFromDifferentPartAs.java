package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.FromDifferentPartAsQuizPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("12")
public class QuizQuestionFromDifferentPartAs extends QuizQuestionEntity {

  public QuizQuestionFromDifferentPartAs(Note note) {
    super(note);
  }

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new FromDifferentPartAsQuizPresenter(this);
  }
}
