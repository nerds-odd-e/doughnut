package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("9")
public class QuizQuestionWhichSpecHasInstance extends QuizQuestionWithNoteChoices {

  public String getMainTopic() {
    return null;
  }
}
