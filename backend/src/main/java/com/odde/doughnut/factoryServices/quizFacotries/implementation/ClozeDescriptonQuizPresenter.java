package com.odde.doughnut.factoryServices.quizFacotries.implementation;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.models.NoteViewer;

public abstract class ClozeDescriptonQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final ReviewPoint reviewPoint;

  public ClozeDescriptonQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
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
}
