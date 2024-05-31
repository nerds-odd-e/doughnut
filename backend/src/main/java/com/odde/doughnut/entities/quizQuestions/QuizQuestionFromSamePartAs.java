package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("10")
public class QuizQuestionFromSamePartAs extends QuizQuestionWithNoteChoices {

  public String getStem() {
    return "<p>Which one <mark>is "
        + getNote().getLinkType().label
        + "</mark> the same "
        + getCategoryLink().getLinkType().nameOfSource
        + " of <mark>"
        + getCategoryLink().getTargetNote().getTopicConstructor()
        + "</mark> as:";
  }

  public String getMainTopic() {
    return getNote().getParent().getTopicConstructor();
  }
}
