package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import java.util.List;
import java.util.stream.Stream;

public class PictureSelectionQuizPresenter extends QuizQuestionWithOptionsPresenter {

  private Note note;

  public PictureSelectionQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.note = quizQuestion.getNote();
  }

  @Override
  public String mainTopic() {
    return note.getTopicConstructor();
  }

  @Override
  public String stem() {
    return "";
  }

  @Override
  protected List<QuizQuestion.Choice> getOptionsFromNote(Stream<Note> noteStream) {
    return noteStream
        .map(
            note -> {
              QuizQuestion.Choice choice = new QuizQuestion.Choice();
              choice.setDisplay(note.getTopicConstructor());
              choice.setPictureWithMask(note.getPictureWithMask().orElse(null));
              choice.setPicture(true);
              return choice;
            })
        .toList();
  }
}
