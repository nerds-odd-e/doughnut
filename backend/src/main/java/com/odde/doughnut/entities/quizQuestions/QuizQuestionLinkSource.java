package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("6")
public class QuizQuestionLinkSource extends QuizQuestionWithNoteChoices {

  public String getStem() {
    return "Which one <em>is immediately " + getNote().getLinkType().label + "</em>:";
  }

  public String getMainTopic() {
    return getNote().getTargetNote().getTopicConstructor();
  }
}
