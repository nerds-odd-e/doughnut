package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.entities.json.RepetitionForUser;
import com.odde.doughnut.entities.json.ReviewStatus;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Reviewing {
  private final UserModel userModel;
  private final Timestamp currentUTCTimestamp;
  private final ModelFactoryService modelFactoryService;

  public Reviewing(
      UserModel user, Timestamp currentUTCTimestamp, ModelFactoryService modelFactoryService) {
    userModel = user;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.modelFactoryService = modelFactoryService;
  }

  public ReviewPoint getOneInitialReviewPoint() {
    return getOneInitialReviewPoint1().findFirst().orElse(null);
  }

  public Stream<ReviewPoint> getOneInitialReviewPoint1() {
    int count = remainingDailyNewNotesCount();
    if (count == 0) {
      return Stream.empty();
    }
    List<Integer> alreadyInitialReviewed =
        getNewReviewPointsOfToday().stream().map(rp -> rp.getSourceNote().getId()).toList();
    return Stream.concat(
            getSubscriptionModelStream()
                .flatMap(
                    sub ->
                        getOneNewReviewPoint(
                            sub, sub.needToLearnCountToday(alreadyInitialReviewed))),
            getOneNewReviewPoint(userModel, count))
        .limit(count);
  }

  private Stream<ReviewPoint> getOneNewReviewPoint(ReviewScope reviewScope, int count) {
    if (count <= 0) return Stream.of();
    Iterator<Note> noteIterator = reviewScope.getNotesHaveNotBeenReviewedAtAll().iterator();
    Iterator<Link> linkIterator = reviewScope.getLinksHaveNotBeenReviewedAtAll().iterator();

    List<ReviewPoint> result = new ArrayList<>();
    for (int cnt = 0; cnt < count; cnt++) {
      Note note = noteIterator.hasNext() ? noteIterator.next() : null;
      Link link = linkIterator.hasNext() ? linkIterator.next() : null;

      if (note == null && link == null) {
        break;
      }
      if (note != null && link != null) {
        if (note.getCreatedAt().compareTo(link.getCreatedAt()) > 0) {
          note = null;
        } else {
          link = null;
        }
      }

      ReviewPoint reviewPoint = new ReviewPoint();
      reviewPoint.setNote(note);
      reviewPoint.setLink(link);
      result.add(reviewPoint);
    }
    return result.stream();
  }

  private int toRepeatCount() {
    return userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp).size();
  }

  private int notLearntCount() {
    Integer subscribedCount =
        getSubscriptionModelStream()
            .map(this::getPendingNewReviewPointCount)
            .reduce(Integer::sum)
            .orElse(0);
    return subscribedCount + getPendingNewReviewPointCount(userModel);
  }

  private int getPendingNewReviewPointCount(ReviewScope reviewScope) {
    int noteCount = reviewScope.getNotesHaveNotBeenReviewedAtAllCount();
    int linkCount = reviewScope.getLinksHaveNotBeenReviewedAtAllCount();

    return noteCount + linkCount;
  }

  public int toInitialReviewCount() {
    ReviewPoint oneInitialReviewPoint = getOneInitialReviewPoint();
    if (oneInitialReviewPoint == null) {
      return 0;
    }
    return Math.min(remainingDailyNewNotesCount(), notLearntCount());
  }

  private int remainingDailyNewNotesCount() {
    long sameDayCount = getNewReviewPointsOfToday().size();
    return (int) (userModel.entity.getDailyNewNotesCount() - sameDayCount);
  }

  private List<ReviewPoint> getNewReviewPointsOfToday() {
    Timestamp oneDayAgo = TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, -24);
    return userModel.getRecentReviewPoints(oneDayAgo).stream()
        .filter(p -> userModel.isInitialReviewOnSameDay(p, currentUTCTimestamp))
        .toList();
  }

  private Optional<ReviewPointModel> getOneReviewPointNeedToRepeat(Randomizer randomizer) {
    List<ReviewPoint> reviewPointsNeedToRepeat =
        userModel.getReviewPointsNeedToRepeat(currentUTCTimestamp);
    return randomizer
        .chooseOneRandomly(reviewPointsNeedToRepeat)
        .map(modelFactoryService::toReviewPointModel);
  }

  private Stream<SubscriptionModel> getSubscriptionModelStream() {
    return userModel.entity.getSubscriptions().stream()
        .map(modelFactoryService::toSubscriptionModel);
  }

  public Optional<RepetitionForUser> getOneRepetitionForUser(Randomizer randomizer) {
    return getOneReviewPointNeedToRepeat(randomizer)
        .map(reviewPointModel -> buildRepetitionForUser(randomizer, reviewPointModel));
  }

  private RepetitionForUser buildRepetitionForUser(
      Randomizer randomizer, ReviewPointModel reviewPointModel) {
    RepetitionForUser repetitionForUser = new RepetitionForUser();
    repetitionForUser.setReviewPoint(reviewPointModel.getEntity());
    repetitionForUser.setQuizQuestion(
        reviewPointModel
            .generateAQuizQuestion(randomizer)
            .map(q -> new QuizQuestionViewedByUser(q, this.modelFactoryService)));
    repetitionForUser.setToRepeatCount(toRepeatCount());
    return repetitionForUser;
  }

  public ReviewStatus getReviewStatus() {
    ReviewStatus reviewStatus = new ReviewStatus();
    reviewStatus.toRepeatCount = toRepeatCount();
    reviewStatus.learntCount = userModel.learntCount();
    reviewStatus.notLearntCount = notLearntCount();
    reviewStatus.toInitialReviewCount = toInitialReviewCount();

    return reviewStatus;
  }
}
