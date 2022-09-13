package com.odde.doughnut.models;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.entities.json.RepetitionForUser;
import com.odde.doughnut.entities.json.ReviewStatus;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
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

  public Stream<ReviewPoint> getDueInitialReviewPoints() {
    int count = remainingDailyNewNotesCount();
    if (count == 0) {
      return Stream.empty();
    }
    List<Integer> alreadyInitialReviewed =
        getNewReviewPointsOfToday().stream().map(ReviewPoint::getThing).map(Thing::getId).toList();
    return Stream.concat(
            getSubscriptionModelStream()
                .flatMap(
                    sub ->
                        getDueNewReviewPoint(
                            sub, sub.needToLearnCountToday(alreadyInitialReviewed))),
            getDueNewReviewPoint(userModel, count))
        .limit(count);
  }

  private Stream<ReviewPoint> getDueNewReviewPoint(ReviewScope reviewScope, int count) {
    return reviewScope
        .getThingHaveNotBeenReviewedAtAll()
        .limit(count)
        .map(ReviewPoint::createReviewPointForThing);
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
    return reviewScope.getThingsHaveNotBeenReviewedAtAllCount();
  }

  private int toInitialReviewCount() {
    if (getDueInitialReviewPoints().findFirst().isEmpty()) {
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
        .filter(p -> !p.getRemovedFromReview())
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
    repetitionForUser.setQuizQuestion(
        new QuizQuestionViewedByUser(
            reviewPointModel.generateAQuizQuestion(randomizer),
            this.modelFactoryService,
            userModel.getEntity()));
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
