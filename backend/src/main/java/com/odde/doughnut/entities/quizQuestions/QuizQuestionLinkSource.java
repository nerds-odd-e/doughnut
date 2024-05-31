package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.QuizQuestionEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("6")
public class QuizQuestionLinkSource extends QuizQuestionEntity {

  public String getMainTopic() {
    return getNote().getTargetNote().getTopicConstructor();
  }
}
