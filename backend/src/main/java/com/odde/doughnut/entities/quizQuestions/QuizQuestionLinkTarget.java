package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.LinkTargetQuizPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("5")
public class QuizQuestionLinkTarget extends QuizQuestionEntity {

  public QuizQuestionLinkTarget(Note note) {
    super(note);
  }

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new LinkTargetQuizPresenter(this);
  }
}
