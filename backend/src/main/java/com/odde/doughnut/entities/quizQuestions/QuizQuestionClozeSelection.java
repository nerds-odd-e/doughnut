package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.QuizQuestionEntity;
import jakarta.persistence.*;

@Entity
@DiscriminatorValue("1")
public class QuizQuestionClozeSelection extends QuizQuestionEntity {

  public String getMainTopic() {
    return "";
  }
}
