package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory extends AbstractCategoryQuizFactory {
  private List<Note> cachedFillingOptions = null;

  public FromDifferentPartAsQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
  }

  @Override
  public int minimumOptionCount() {
    return 2;
  }

  @Override
  public List<Note> allWrongAnswers() {
    List<Note> result =
        new ArrayList<>(reviewPoint.getLink().getCousinsOfSameLinkType(reviewPoint.getUser()));
    result.add(link.getSourceNote());
    return result;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      List<Link> cousinLinks = link.getCousinLinksOfSameLinkType(reviewPoint.getUser());
      cachedFillingOptions =
          servant.randomizer.randomlyChoose(5, cousinLinks).stream()
              .map(Link::getSourceNote)
              .collect(Collectors.toList());
    }
    return cachedFillingOptions;
  }

  @Override
  public Link getCategoryLink() {
    return this.categoryLink;
  }

  @Override
  public Note generateAnswerNote() {
    User user = reviewPoint.getUser();
    return servant
        .randomizer
        .chooseOneRandomly(getReverseLinksOfCousins(user))
        .map(Link::getSourceNote)
        .orElse(null);
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    return getCategoryReviewPoints(userModel);
  }
}
