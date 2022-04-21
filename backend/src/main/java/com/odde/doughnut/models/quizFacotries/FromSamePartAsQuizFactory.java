package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FromSamePartAsQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  private Link cachedAnswerLink = null;
  private List<Note> cachedFillingOptions = null;
  protected final ReviewPoint reviewPoint;
  protected final Link link;
  private Optional<Link> categoryLink = null;

  public FromSamePartAsQuizFactory(ReviewPoint reviewPoint) {
    this.reviewPoint = reviewPoint;
    this.link = reviewPoint.getLink();
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    if (cachedFillingOptions == null) {
      categoryLink = servant.chooseOneCategoryLink(reviewPoint.getUser(), link);
      cachedFillingOptions =
          categoryLink
              .map(lk -> lk.getReverseLinksOfCousins(reviewPoint.getUser(), link.getLinkType()))
              .map(
                  remoteCousins ->
                      servant.randomizer.randomlyChoose(5, remoteCousins).stream()
                          .map(Link::getSourceNote)
                          .collect(Collectors.toList()))
              .orElse(Collections.emptyList());
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswerNote(QuizQuestionServant servant) {
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
    if (cachedAnswerLink != null) {
      ReviewPoint answerLinkReviewPoint = userModel.getReviewPointFor(cachedAnswerLink);
      List<ReviewPoint> result = new ArrayList<>();
      result.add(answerLinkReviewPoint);
      categoryLink.map(userModel::getReviewPointFor).ifPresent(result::add);
      return result;
    }
    return Collections.emptyList();
  }

  @Override
  public List<Note> knownRightAnswers() {
    return reviewPoint.getLink().getCousinOfSameLinkType(reviewPoint.getUser());
  }

  @Override
  public Link getCategoryLink() {
    return this.categoryLink.orElse(null);
  }

  protected Link getAnswerLink(QuizQuestionServant servant) {
    if (cachedAnswerLink == null) {
      UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
      List<Link> backwardPeers =
          link.getCousinLinksOfSameLinkType(reviewPoint.getUser()).stream()
              .filter(l -> userModel.getReviewPointFor(l) != null)
              .toList();
      cachedAnswerLink = servant.randomizer.chooseOneRandomly1(backwardPeers).orElse(null);
    }
    return cachedAnswerLink;
  }
}
