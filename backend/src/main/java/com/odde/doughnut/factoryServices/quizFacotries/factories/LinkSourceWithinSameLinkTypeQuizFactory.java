package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionLinkSourceWithSameLinkType;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkSourceWithinSameLinkTypeQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final LinkingNote link;
  private List<LinkingNote> cachedFillingOptions = null;

  public LinkSourceWithinSameLinkTypeQuizFactory(LinkingNote note) {
    this.link = note;
  }

  @Override
  public List<LinkingNote> generateFillingOptions(QuizQuestionServant servant) {
    if (cachedFillingOptions == null) {
      cachedFillingOptions =
          servant.chooseFromCohortAvoidSiblings(link).stream()
              .flatMap(n -> servant.randomizer.chooseOneRandomly(n.getLinks()).stream())
              .toList();
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
    return link;
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    QuizQuestionLinkSourceWithSameLinkType quizQuestionLinkSourceWithSameLinkType =
        new QuizQuestionLinkSourceWithSameLinkType();
    quizQuestionLinkSourceWithSameLinkType.setNote(link);
    return quizQuestionLinkSourceWithSameLinkType;
  }
}
