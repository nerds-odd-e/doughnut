package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FromSamePartAsQuizFactory extends AbstractCategoryQuizFactory {
  private Link cachedAnswerLink = null;
  private List<Note> cachedFillingOptions = null;

  public FromSamePartAsQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      List<Link> remoteCousins = getReverseLinksOfCousins(reviewPoint.getUser());
      cachedFillingOptions =
          servant.randomizer.randomlyChoose(5, remoteCousins).stream()
              .map(Link::getSourceNote)
              .collect(Collectors.toList());
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswerNote() {
    if (getAnswerLink(servant) == null) return null;
    return getAnswerLink(servant).getSourceNote();
  }

  @Override
  public int minimumOptionCount() {
    return 2;
  }

  @Override
  public int minimumViceReviewPointCount() {
    return 1;
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    if (cachedAnswerLink == null) {
      return Collections.emptyList();
    }
    ReviewPoint answerLinkReviewPoint = userModel.getReviewPointFor(cachedAnswerLink);
    List<ReviewPoint> result = new ArrayList<>();
    result.add(answerLinkReviewPoint);
    result.addAll(getCategoryReviewPoints(userModel));
    return result;
  }

  @Override
  public List<Note> knownRightAnswers() {
    return reviewPoint.getLink().getCousinsOfSameLinkType(reviewPoint.getUser());
  }

  @Override
  public Link getCategoryLink() {
    return this.categoryLink;
  }

  protected Link getAnswerLink(QuizQuestionServant servant) {
    if (cachedAnswerLink == null) {
      List<Link> backwardPeers = getCousinLinksFromSameCategoriesOfSameLinkType().toList();
      cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers).orElse(null);
    }
    return cachedAnswerLink;
  }
}
