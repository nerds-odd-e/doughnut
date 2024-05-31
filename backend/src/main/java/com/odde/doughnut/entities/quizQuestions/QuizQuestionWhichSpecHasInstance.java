package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("9")
public class QuizQuestionWhichSpecHasInstance extends QuizQuestionWithNoteChoices {

  public String getStem() {
    return "<p>Which one is "
        + getNote().getLinkType().label
        + " <mark>"
        + getNote().getTargetNote().getTopicConstructor()
        + "</mark> <em>and</em> is "
        + getCategoryLink().getLinkType().label
        + " <mark>"
        + getCategoryLink().getTargetNote().getTopicConstructor()
        + "</mark>:";
  }

  public String getMainTopic() {
    return null;
  }
}
