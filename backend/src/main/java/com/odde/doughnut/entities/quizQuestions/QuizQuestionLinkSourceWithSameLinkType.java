package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.QuizQuestionEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("14")
public class QuizQuestionLinkSourceWithSameLinkType extends QuizQuestionEntity {

  @Override
  public String getMainTopic() {
    return getNote().getTargetNote().getTopicConstructor();
  }
}
