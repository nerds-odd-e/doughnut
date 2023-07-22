package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.models.NoteViewer;

public abstract class ClozeDescriptonQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final ReviewPoint reviewPoint;

  public ClozeDescriptonQuizPresenter(QuizQuestionEntity quizQuestion) {
    this.reviewPoint = quizQuestion.getReviewPoint();
  }

  @Override
  public String mainTopic() {
    return "";
  }

  @Override
  public String instruction() {
    return reviewPoint.getNote().getClozeDescription().cloze();
  }

  @Override
  public LinksOfANote hintLinks() {
    NoteViewer noteViewer = new NoteViewer(reviewPoint.getUser(), reviewPoint.getNote());
    return LinksOfANote.getOpenLinksOfANote(noteViewer);
  }

  @Override
  public boolean isAnswerCorrect(Answer answer) {
    return reviewPoint.getNote().getId().equals(answer.getAnswerNoteId());
  }
}
