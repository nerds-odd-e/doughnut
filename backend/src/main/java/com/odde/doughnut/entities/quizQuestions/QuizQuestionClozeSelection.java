package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("1")
public class QuizQuestionClozeSelection extends QuizQuestionWithNoteChoices {

  public String getMainTopic() {
    return "";
  }
}
