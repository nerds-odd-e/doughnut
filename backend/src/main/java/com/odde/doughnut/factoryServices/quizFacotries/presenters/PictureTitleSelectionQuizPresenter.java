package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import java.util.Optional;

public class PictureTitleSelectionQuizPresenter extends ClozeTitleSelectionQuizPresenter {
  private Note note;

  public PictureTitleSelectionQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    note = quizQuestion.getThing().getNote();
  }

  @Override
  public Optional<PictureWithMask> pictureWithMask() {
    return note.getPictureWithMask();
  }
}
