package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("7")
public class QuizQuestionClozeLinkTarget extends QuizQuestionWithNoteChoices {

  public String getMainTopic() {
    return "";
  }
}
