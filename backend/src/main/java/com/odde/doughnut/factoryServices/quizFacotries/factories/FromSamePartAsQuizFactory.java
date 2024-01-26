package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;
import java.util.stream.Collectors;

public class FromSamePartAsQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {

  private final ParentGrandLinkHelper parentGrandLinkHelper;
  private Link cachedAnswerLink = null;
  private List<Note> cachedFillingOptions = null;
  private final Link link;
  private final QuizQuestionServant servant;

  public FromSamePartAsQuizFactory(Thing thing, QuizQuestionServant servant) {
    link = thing.getLink();
    this.servant = servant;
    parentGrandLinkHelper = servant.getParentGrandLinkHelper(link);
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      List<Link> remoteCousins = parentGrandLinkHelper.getCousinLinksAvoidingSiblings();
      cachedFillingOptions =
          servant.chooseFillingOptionsRandomly(remoteCousins).stream()
              .map(Link::getSourceNote)
              .collect(Collectors.toList());
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    if (getAnswerLink() == null) return null;
    return getAnswerLink().getSourceNote();
  }

  @Override
  public Link getCategoryLink() {
    return parentGrandLinkHelper.getParentGrandLink();
  }

  protected Link getAnswerLink() {
    if (cachedAnswerLink == null) {
      List<Link> backwardPeers =
          servant.getSiblingLinksOfSameLinkTypeHavingReviewPoint(link).toList();
      cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers).orElse(null);
    }
    return cachedAnswerLink;
  }
}
