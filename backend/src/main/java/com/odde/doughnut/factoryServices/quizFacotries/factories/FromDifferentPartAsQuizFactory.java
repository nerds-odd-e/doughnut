package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FromDifferentPartAsQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {

  private final ParentGrandLinkHelper parentGrandLinkHelper;
  private final Link link;
  private final QuizQuestionServant servant;

  public FromDifferentPartAsQuizFactory(Thing thing, QuizQuestionServant servant) {
    link = thing.getLink();
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
    Stream<Link> cousinLinks = servant.getSiblingLinksOfSameLinkTypeHavingReviewPoint(link);
    return servant
        .chooseFillingOptionsRandomly(cousinLinks)
        .map(Link::getSourceNote)
        .collect(Collectors.toList());
  }

  @Override
  public Link getCategoryLink() {
    return parentGrandLinkHelper.getParentGrandLink();
  }

  @Override
  public Note generateAnswer() {
    return servant
        .randomizer
        .chooseOneRandomly(parentGrandLinkHelper.getCousinLinksAvoidingSiblings().toList())
        .map(Link::getSourceNote)
        .orElse(null);
  }
}
