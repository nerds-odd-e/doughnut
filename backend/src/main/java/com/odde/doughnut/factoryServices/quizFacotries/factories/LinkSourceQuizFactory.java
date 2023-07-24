package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkSourceQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final Link link;
  private QuizQuestionServant servant;
  protected final Note answerNote;
  private List<Note> cachedFillingOptions = null;

  public LinkSourceQuizFactory(Thing thing, QuizQuestionServant servant) {
    this.link = thing.getLink();
    this.servant = servant;
    this.answerNote = link.getSourceNote();
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      cachedFillingOptions = servant.chooseFromCohortAvoidSiblings(link, answerNote);
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    return answerNote;
  }
}
