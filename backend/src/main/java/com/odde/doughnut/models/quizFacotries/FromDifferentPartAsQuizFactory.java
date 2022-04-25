package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {

  private final CategoryHelper categoryHelper;
  private final User user;
  private final Link link;
  private final QuizQuestionServant servant;

  public FromDifferentPartAsQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    user = reviewPoint.getUser();
    link = reviewPoint.getLink();
    this.servant = servant;
    categoryHelper = new CategoryHelper(servant, user, link);
  }

  @Override
  public List<Note> allWrongAnswers() {
    List<Note> result = new ArrayList<>(link.getCousinsOfSameLinkType(user));
    result.add(link.getSourceNote());
    return result;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (getCategoryLink() == null) {
      return null;
    }
    List<Link> cousinLinks =
        servant
            .getCousinLinksOfSameLinkTypeHavingReviewPoint(link, user)
            .collect(Collectors.toList());
    return servant.chooseFillingOptionsRandomly(cousinLinks).stream()
        .map(Link::getSourceNote)
        .collect(Collectors.toList());
  }

  @Override
  public Link getCategoryLink() {
    return categoryHelper.getCategoryLink();
  }

  @Override
  public Note generateAnswer() {
    return servant
        .randomizer
        .chooseOneRandomly(categoryHelper.getReverseLinksOfCousins())
        .map(Link::getSourceNote)
        .orElse(null);
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints() {
    return categoryHelper.getCategoryReviewPoints();
  }
}
