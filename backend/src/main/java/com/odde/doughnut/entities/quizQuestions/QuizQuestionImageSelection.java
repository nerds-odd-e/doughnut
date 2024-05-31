package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("4")
public class QuizQuestionImageSelection extends QuizQuestionWithNoteChoices {

  @Override
  public String getMainTopic() {
    return getNote().getTopicConstructor();
  }

  public String getStem() {
    return "";
  }
}
