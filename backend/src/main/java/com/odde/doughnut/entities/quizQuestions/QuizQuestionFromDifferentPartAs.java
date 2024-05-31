package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.QuizQuestionEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("12")
public class QuizQuestionFromDifferentPartAs extends QuizQuestionEntity {

  public String getMainTopic() {
    return getNote().getParent().getTopicConstructor();
  }
}
