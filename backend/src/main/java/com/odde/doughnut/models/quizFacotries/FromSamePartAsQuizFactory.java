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

public class FromSamePartAsQuizFactory extends AbstractCategoryQuizFactory {
  private Link cachedAnswerLink = null;
  private List<Note> cachedFillingOptions = null;
  private Link categoryLink = null;

  public FromSamePartAsQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
    if (servant != null) {
    categoryLink = servant.chooseOneCategoryLink(reviewPoint.getUser(), link).orElse(null);
    } else {
      categoryLink = null;
    }
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      cachedFillingOptions =
          Optional.ofNullable(categoryLink)
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
    if (cachedAnswerLink != null) {
      ReviewPoint answerLinkReviewPoint = userModel.getReviewPointFor(cachedAnswerLink);
      List<ReviewPoint> result = new ArrayList<>();
      result.add(answerLinkReviewPoint);
      if(categoryLink != null) {
        ReviewPoint reviewPointFor = userModel.getReviewPointFor(categoryLink);
        if(reviewPointFor != null) result.add(reviewPointFor);
      }
      return result;
    }
    return Collections.emptyList();
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
      UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
      List<Link> backwardPeers =
          link.getCousinLinksOfSameLinkType(reviewPoint.getUser()).stream()
              .filter(l -> userModel.getReviewPointFor(l) != null)
              .toList();
      cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers).orElse(null);
    }
    return cachedAnswerLink;
  }
}
