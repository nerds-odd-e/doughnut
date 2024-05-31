package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("5")
public class QuizQuestionLinkTarget extends QuizQuestionWithNoteChoices {

  public String getStem() {
    return "<mark>"
        + getNote().getParent().getTopicConstructor()
        + "</mark> is "
        + getNote().getLinkType().label
        + ":";
  }

  public String getMainTopic() {
    return "";
  }
}
