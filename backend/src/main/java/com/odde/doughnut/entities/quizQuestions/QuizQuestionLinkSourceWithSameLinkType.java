package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.Note;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("14")
public class QuizQuestionLinkSourceWithSameLinkType extends QuizQuestionWithNoteChoices {

  @Override
  public String getMainTopic() {
    return getNote().getTargetNote().getTopicConstructor();
  }

  @Override
  public String getStem() {
    return "Which one <em>is immediately " + getNote().getLinkType().label + "</em>:";
  }

  @Override
  protected QuizQuestion.Choice noteToChoice(Note note) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    Note source = note.getParent();
    Note target = note.getTargetNote();
    choice.setDisplay(
        ClozedString.htmlClozedString(source.getTopicConstructor())
            .hide(target.getNoteTitle())
            .clozeTitle());
    return choice;
  }
}
