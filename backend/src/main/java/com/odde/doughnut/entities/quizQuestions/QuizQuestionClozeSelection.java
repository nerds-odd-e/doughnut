package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.*;
import jakarta.persistence.*;

@Entity
@DiscriminatorValue("1")
public class QuizQuestionClozeSelection extends QuizQuestionWithNoteChoices {

  public String getStem() {
    return getNote().getClozeDescription().clozeDetails();
  }

  public String getMainTopic() {
    return "";
  }
}
