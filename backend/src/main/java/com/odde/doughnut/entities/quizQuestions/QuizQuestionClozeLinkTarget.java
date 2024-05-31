package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.algorithms.ClozedString;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("7")
public class QuizQuestionClozeLinkTarget extends QuizQuestionWithNoteChoices {
  public String getStem() {
    ClozedString clozeTitle =
        ClozedString.htmlClozedString(getNote().getParent().getTopicConstructor())
            .hide(getNote().getTargetNote().getNoteTitle());
    return "<mark>" + clozeTitle.clozeTitle() + "</mark> is " + getNote().getLinkType().label + ":";
  }

  public String getMainTopic() {
    return "";
  }
}
