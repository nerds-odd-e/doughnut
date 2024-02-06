package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkSourceWithinSameLinkTypeQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final LinkingNote link;
  private final QuizQuestionServant servant;
  private List<LinkingNote> cachedFillingOptions = null;

  public LinkSourceWithinSameLinkTypeQuizFactory(LinkingNote note, QuizQuestionServant servant) {
    this.link = note;
    this.servant = servant;
  }

  @Override
  public List<LinkingNote> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      cachedFillingOptions =
          servant.chooseFromCohortAvoidSiblings(link).stream()
              .flatMap(n -> servant.randomizer.chooseOneRandomly(n.getLinks()).stream())
              .toList();
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    return link;
  }
}
