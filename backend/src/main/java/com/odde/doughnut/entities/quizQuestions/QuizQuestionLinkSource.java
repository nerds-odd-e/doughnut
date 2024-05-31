package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("6")
public class QuizQuestionLinkSource extends QuizQuestionWithNoteChoices {

  public String getMainTopic() {
    return getNote().getTargetNote().getTopicConstructor();
  }
}
