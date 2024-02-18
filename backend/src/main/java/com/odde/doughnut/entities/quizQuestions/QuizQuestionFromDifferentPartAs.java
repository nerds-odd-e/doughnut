package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("12")
public class QuizQuestionFromDifferentPartAs extends QuizQuestionWithNoteChoices {

  public String getStem() {
    return "<p>Which one <mark>is "
        + getNote().getLinkType().label
        + "</mark> a <em>DIFFERENT</em> "
        + getCategoryLink().getLinkType().nameOfSource
        + " of <mark>"
        + getCategoryLink().getTargetNote().getTopicConstructor()
        + "</mark> than:";
  }

  public String getMainTopic() {
    return getNote().getParent().getTopicConstructor();
  }
}
