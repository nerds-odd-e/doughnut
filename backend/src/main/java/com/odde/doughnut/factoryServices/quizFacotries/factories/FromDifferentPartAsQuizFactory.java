package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {

  private final ParentGrandLinkHelper parentGrandLinkHelper;
  private final Note link;
  private final QuizQuestionServant servant;

  public FromDifferentPartAsQuizFactory(Note note, QuizQuestionServant servant) {
    link = note;
    this.servant = servant;
    parentGrandLinkHelper = servant.getParentGrandLinkHelper(link);
  }

  @Override
  public int minimumOptionCount() {
    return 3;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (getCategoryLink() == null) {
      return null;
    }
    List<Note> cousinLinks =
        servant
            .getSiblingLinksOfSameLinkTypeHavingReviewPoint(link.getThing())
            .collect(Collectors.toList());
    return servant.chooseFillingOptionsRandomly(cousinLinks).stream()
        .map(Note::getParent)
        .collect(Collectors.toList());
  }

  @Override
  public Note getCategoryLink() {
    return parentGrandLinkHelper.getParentGrandLink();
  }

  @Override
  public Note generateAnswer() {
    return servant
        .randomizer
        .chooseOneRandomly(parentGrandLinkHelper.getCousinLinksAvoidingSiblings())
        .map(Note::getParent)
        .orElse(null);
  }
}
