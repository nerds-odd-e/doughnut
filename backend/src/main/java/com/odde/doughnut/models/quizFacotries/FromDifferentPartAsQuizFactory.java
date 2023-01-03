package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {

  private final ParentGrandLinkHelper parentGrandLinkHelper;
  private final User user;
  private final Link link;
  private final QuizQuestionServant servant;

  public FromDifferentPartAsQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    user = reviewPoint.getUser();
    link = reviewPoint.getLink();
    this.servant = servant;
    parentGrandLinkHelper = servant.getParentGrandLinkHelper(user, link);
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
    List<Link> cousinLinks =
        servant
            .getSiblingLinksOfSameLinkTypeHavingReviewPoint(link, user)
            .collect(Collectors.toList());
    return servant.chooseFillingOptionsRandomly(cousinLinks).stream()
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
        .chooseOneRandomly(parentGrandLinkHelper.getCousinLinksAvoidingSiblings())
        .map(Link::getSourceNote)
        .orElse(null);
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints() {
    return servant.getReviewPoints(getCategoryLink(), user);
  }
}
