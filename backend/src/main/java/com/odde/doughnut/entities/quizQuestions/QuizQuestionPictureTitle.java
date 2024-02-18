package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.PictureTitleSelectionQuizPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.QuizQuestionWithOptionsPresenter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Optional;

@Entity
@DiscriminatorValue("3")
public class QuizQuestionPictureTitle extends QuizQuestionWithNoteChoices {

  @JsonIgnore
  public QuizQuestionWithOptionsPresenter buildPresenter() {
    return new PictureTitleSelectionQuizPresenter(this);
  }

  @Override
  public Optional<PictureWithMask> getPictureWithMask() {
    return getNote().getPictureWithMask();
  }

  public String getStem() {
    return buildPresenter().stem();
  }

  public String getMainTopic() {
    return buildPresenter().mainTopic();
  }
}
