package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.models.NoteViewer;
import java.util.List;

public abstract class ClozeDescriptonQuizPresenter implements QuizQuestionPresenter {
  private final ReviewPoint reviewPoint;

  public ClozeDescriptonQuizPresenter(QuizQuestion quizQuestion) {
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
  public List<Note> knownRightAnswers() {
    return List.of(reviewPoint.getNote());
  }
}
