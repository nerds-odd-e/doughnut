package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("4")
public class QuizQuestionPictureSelection extends QuizQuestionWithNoteChoices {

  @Override
  public String getMainTopic() {
    return getNote().getTopicConstructor();
  }

  @Override
  protected QuizQuestion.Choice noteToChoice(Note note) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    choice.setDisplay(note.getTopicConstructor());
    choice.setImageWithMask(note.getImageWithMask().orElse(null));
    choice.setPicture(true);
    return choice;
  }

  public String getStem() {
    return "";
  }
}
