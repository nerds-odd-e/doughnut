package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.LinkSourceQuizPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("6")
public class QuizQuestionLinkSource extends QuizQuestionWithNoteChoices {
  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new LinkSourceQuizPresenter(this);
  }
}
