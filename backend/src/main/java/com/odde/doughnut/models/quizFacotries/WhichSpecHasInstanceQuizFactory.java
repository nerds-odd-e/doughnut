package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WhichSpecHasInstanceQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {
  private Link cachedInstanceLink = null;
  private List<Note> cachedFillingOptions = null;
  private final ReviewPoint reviewPoint;
  private final Link link;
  private final QuizQuestionServant servant;

  public WhichSpecHasInstanceQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.link = reviewPoint.getLink();
    this.servant = servant;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions != null) {
      return cachedFillingOptions;
    }
    this.cachedFillingOptions = servant.chooseBackwardPeers(cachedInstanceLink, link);
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    Link instanceLink = getInstanceLink();
    if (instanceLink == null) return null;
    return instanceLink.getSourceNote();
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints() {
    UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
    Link instanceLink = getInstanceLink();
    if (instanceLink != null) {
      ReviewPoint reviewPointFor = userModel.getReviewPointFor(instanceLink);
      if (reviewPointFor != null) {
        return List.of(reviewPointFor);
      }
    }
    return Collections.emptyList();
  }

  private Link getInstanceLink() {
    if (cachedInstanceLink == null) {
      Stream<Link> candidates = servant.getLinksFromSameSourceHavingReviewPoint(link);
      cachedInstanceLink =
          servant
              .randomizer
              .chooseOneRandomly(candidates.collect(Collectors.toList()))
              .orElse(null);
    }
    return cachedInstanceLink;
  }

  @Override
  public Link getCategoryLink() {
    return getInstanceLink();
  }
}
