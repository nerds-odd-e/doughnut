package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.ImageWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("3")
public class QuizQuestionImageTitle extends QuizQuestionEntity {

  @Override
  public ImageWithMask getImageWithMask() {
    return getNote().getImageWithMask();
  }

  public String getMainTopic() {
    return "";
  }
}
