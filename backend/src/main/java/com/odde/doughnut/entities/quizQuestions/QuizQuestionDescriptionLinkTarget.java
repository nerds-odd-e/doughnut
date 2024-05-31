package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("8")
public class QuizQuestionDescriptionLinkTarget extends QuizQuestionWithNoteChoices {

  public String getMainTopic() {
    return "";
  }
}
