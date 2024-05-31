package com.odde.doughnut.entities.quizQuestions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("14")
public class QuizQuestionLinkSourceWithSameLinkType extends QuizQuestionWithNoteChoices {

  @Override
  public String getMainTopic() {
    return getNote().getTargetNote().getTopicConstructor();
  }
}
