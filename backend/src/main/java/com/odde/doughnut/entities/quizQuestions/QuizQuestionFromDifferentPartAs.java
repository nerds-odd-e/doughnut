package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("12")
public class QuizQuestionFromDifferentPartAs extends QuizQuestionWithNoteChoices {

  public String getMainTopic() {
    return getNote().getParent().getTopicConstructor();
  }
}
