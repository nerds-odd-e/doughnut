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
    implements QuizQuestionFactory, QuestionOptionsFactory {
  private Link cachedInstanceLink = null;
  private List<Note> cachedFillingOptions = null;
  private final ReviewPoint reviewPoint;
  private final Link link;
  private QuizQuestionServant servant;

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
    List<Note> instanceReverse = cachedInstanceLink.getCousinsOfSameLinkType(reviewPoint.getUser());
    List<Note> specReverse = link.getCousinsOfSameLinkType(reviewPoint.getUser());
    List<Note> backwardPeers =
        Stream.concat(instanceReverse.stream(), specReverse.stream())
            .filter(n -> !(instanceReverse.contains(n) && specReverse.contains(n)))
            .collect(Collectors.toList());
    cachedFillingOptions = servant.chooseFillingOptionsRandomly(backwardPeers);
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    cachedInstanceLink = getInstanceLink(servant);
    if (cachedInstanceLink == null) return null;
    return cachedInstanceLink.getSourceNote();
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    if (cachedInstanceLink != null) {
      ReviewPoint reviewPointFor = userModel.getReviewPointFor(cachedInstanceLink);
      if (reviewPointFor != null) {
        return List.of(reviewPointFor);
      }
    }
    return Collections.emptyList();
  }

  private Link getInstanceLink(QuizQuestionServant servant) {
    if (cachedInstanceLink == null) {
      Stream<Link> candidates =
          servant.getLinksFromSameSourceHavingReviewPoint(reviewPoint.getUser(), link);
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
    return cachedInstanceLink;
  }

  @Override
  public List<Note> knownRightAnswers() {
    return List.of(link.getSourceNote());
  }
}
