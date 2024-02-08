package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.FromSamePartAsQuizPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("10")
public class QuizQuestionFromSamePartAs extends QuizQuestionEntity {
  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new FromSamePartAsQuizPresenter(this);
  }
}
