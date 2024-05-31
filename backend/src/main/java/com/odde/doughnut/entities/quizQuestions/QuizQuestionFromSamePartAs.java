package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("10")
public class QuizQuestionFromSamePartAs extends QuizQuestionWithNoteChoices {

  public String getMainTopic() {
    return getNote().getParent().getTopicConstructor();
  }
}
