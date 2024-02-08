package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.WhichSpecHasInstanceQuizPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("9")
public class QuizQuestionWhichSpecHasInstance extends QuizQuestionEntity {

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new WhichSpecHasInstanceQuizPresenter(this);
  }
}
