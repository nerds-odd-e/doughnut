package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FromSamePartAsQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {

  private final ParentGrandLinkHelper parentGrandLinkHelper;
  private Link cachedAnswerLink = null;
  private List<Note> cachedFillingOptions = null;
  private final User user;
  private final Link link;
  private final QuizQuestionServant servant;

  public FromSamePartAsQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    user = reviewPoint.getUser();
    link = reviewPoint.getLink();
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
  public List<ReviewPoint> getViceReviewPoints() {
    Link answerLink = this.getAnswerLink();
    if (answerLink == null) {
      return Collections.emptyList();
    }
    ReviewPoint answerLinkReviewPoint = servant.getReviewPoint(answerLink.getThing());
    List<ReviewPoint> result = new ArrayList<>();
    result.add(answerLinkReviewPoint);
    result.addAll(servant.getReviewPoints(parentGrandLinkHelper.getParentGrandLink()));
    return result;
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
