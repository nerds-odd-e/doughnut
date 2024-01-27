package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkSourceQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final Link link;
  private QuizQuestionServant servant;
  private List<Note> cachedFillingOptions = null;

  public LinkSourceQuizFactory(Thing thing, QuizQuestionServant servant) {
    this.link = thing.getLink();
    this.servant = servant;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      cachedFillingOptions = servant.chooseFromCohortAvoidSiblings(link);
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    return link.getSourceNote();
  }
}
