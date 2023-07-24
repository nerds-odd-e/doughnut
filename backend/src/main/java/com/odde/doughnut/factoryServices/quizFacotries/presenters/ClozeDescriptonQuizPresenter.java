package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.models.NoteViewer;

public abstract class ClozeDescriptonQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Note note;

  public ClozeDescriptonQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.note = quizQuestion.getThing().getNote();
  }

  @Override
  public String mainTopic() {
    return "";
  }

  @Override
  public String instruction() {
    return note.getClozeDescription().cloze();
  }

  @Override
  public LinksOfANote hintLinks(User user) {
    NoteViewer noteViewer = new NoteViewer(user, note);
    return LinksOfANote.getOpenLinksOfANote(noteViewer);
  }
}
