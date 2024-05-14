package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.ImageWithMask;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("3")
public class QuizQuestionImageTitle extends QuizQuestionWithNoteChoices {

  @Override
  public ImageWithMask getImageWithMask() {
    return getNote().getImageWithMask();
  }

  public String getStem() {
    return getNote().getClozeDescription().clozeDetails();
  }

  public String getMainTopic() {
    return "";
  }
}
