package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.FromSamePartAsQuizPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.QuizQuestionWithOptionsPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("10")
public class QuizQuestionFromSamePartAs extends QuizQuestionWithNoteChoices {

  @JsonIgnore
  public QuizQuestionWithOptionsPresenter buildPresenter() {
    return new FromSamePartAsQuizPresenter(this);
  }

  public String getStem() {
    return buildPresenter().stem();
  }

  public String getMainTopic() {
    return buildPresenter().mainTopic();
  }
}
