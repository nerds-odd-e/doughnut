package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.algorithms.ClozedString;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("8")
public class QuizQuestionDescriptionLinkTarget extends QuizQuestionWithNoteChoices {

  public String getStem() {
    ClozedString clozeDescription =
        getNote().getParent().getClozeDescription().hide(getNote().getTargetNote().getNoteTitle());
    return "<p>The following descriptions is "
        + getNote().getLinkType().label
        + ":</p>"
        + clozeDescription.clozeDetails();
  }

  public String getMainTopic() {
    return "";
  }
}
