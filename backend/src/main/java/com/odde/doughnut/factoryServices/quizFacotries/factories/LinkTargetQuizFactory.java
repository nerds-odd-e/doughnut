package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkTargetQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final LinkingNote link;
  private QuizQuestionServant servant;
  protected final Note answerNote;
  private List<Note> cachedFillingOptions = null;

  public LinkTargetQuizFactory(LinkingNote note, QuizQuestionServant servant) {
    this.link = note;
    this.servant = servant;
    this.answerNote = link.getTargetNote();
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      cachedFillingOptions = servant.chooseFromCohortAvoidUncles(link, answerNote);
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    return answerNote;
  }
}
