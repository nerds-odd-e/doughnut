package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Optional;

@Entity
@DiscriminatorValue("3")
public class QuizQuestionPictureTitle extends QuizQuestionWithNoteChoices {

  @Override
  public Optional<PictureWithMask> getPictureWithMask() {
    return getNote().getPictureWithMask();
  }

  public String getStem() {
    return getNote().getClozeDescription().clozeDetails();
  }

  public String getMainTopic() {
    return "";
  }
}
