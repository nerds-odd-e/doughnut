package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.LinkSourceWithinSameLinkTypeQuizPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("14")
public class QuizQuestionLinkSourceWithSameLinkType extends QuizQuestionEntity {

  public QuizQuestionLinkSourceWithSameLinkType(Note note) {
    super(note);
  }

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new LinkSourceWithinSameLinkTypeQuizPresenter(this);
  }
}
