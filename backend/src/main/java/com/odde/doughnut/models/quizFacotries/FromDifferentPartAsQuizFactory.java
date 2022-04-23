package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {

  private final CategoryHelper categoryHelper;
  private List<Note> cachedFillingOptions = null;
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
    if (cachedFillingOptions == null) {
      List<Link> cousinLinks =
          categoryHelper
              .getCousinLinksFromSameCategoriesOfSameLinkType()
              .collect(Collectors.toList());
      cachedFillingOptions =
          servant.chooseFillingOptionsRandomly(cousinLinks).stream()
              .map(Link::getSourceNote)
              .collect(Collectors.toList());
    }
    return cachedFillingOptions;
  }

  @Override
  public Link getCategoryLink() {
    return categoryHelper.getCategoryLink();
  }

  @Override
  public Note generateAnswerNote() {
    return servant
        .randomizer
        .chooseOneRandomly(categoryHelper.getReverseLinksOfCousins())
        .map(Link::getSourceNote)
        .orElse(null);
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    return categoryHelper.getCategoryReviewPoints(userModel);
  }
}
