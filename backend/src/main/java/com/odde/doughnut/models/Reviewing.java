package com.odde.doughnut.models;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.json.DueReviewPoints;
import com.odde.doughnut.entities.json.ReviewStatus;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
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

  public Stream<ReviewPoint> getDueInitialReviewPoints(ZoneId timeZone) {
    int count = remainingDailyNewNotesCount(timeZone);
    if (count == 0) {
      return Stream.empty();
    }
    List<Integer> alreadyInitialReviewed =
        getNewReviewPointsOfToday(timeZone).stream()
            .map(ReviewPoint::getThing)
            .map(Thing::getId)
            .toList();
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
        .map(ReviewPoint::buildReviewPointForThing);
  }

  private Stream<ReviewPoint> getReviewPointsNeedToRepeat(int dueInDays, ZoneId timeZone) {
    return userModel.getReviewPointsNeedToRepeat(
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24), timeZone);
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

  private int toInitialReviewCount(ZoneId timeZone) {
    if (getDueInitialReviewPoints(timeZone).findFirst().isEmpty()) {
      return 0;
    }
    return Math.min(remainingDailyNewNotesCount(timeZone), notLearntCount());
  }

  private int remainingDailyNewNotesCount(ZoneId timeZone) {
    long sameDayCount = getNewReviewPointsOfToday(timeZone).size();
    return (int) (userModel.entity.getDailyNewNotesCount() - sameDayCount);
  }

  private List<ReviewPoint> getNewReviewPointsOfToday(ZoneId timeZone) {
    Timestamp oneDayAgo = TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, -24);
    return userModel.getRecentReviewPoints(oneDayAgo).stream()
        .filter(p -> userModel.isInitialReviewOnSameDay(p, currentUTCTimestamp, timeZone))
        .filter(p -> !p.getRemovedFromReview())
        .toList();
  }

  private Stream<SubscriptionModel> getSubscriptionModelStream() {
    return userModel.entity.getSubscriptions().stream()
        .map(modelFactoryService::toSubscriptionModel);
  }

  public DueReviewPoints getDueReviewPoints(Integer dueInDays, ZoneId timeZone) {
    List<Integer> toRepeat =
        getReviewPointsNeedToRepeat(dueInDays == null ? 0 : dueInDays, timeZone)
            .map(ReviewPoint::getId)
            .toList();
    DueReviewPoints dueReviewPoints = new DueReviewPoints();
    dueReviewPoints.setDueInDays(dueInDays);
    dueReviewPoints.setToRepeat(toRepeat);
    return dueReviewPoints;
  }

  public ReviewStatus getReviewStatus(ZoneId timeZone) {
    ReviewStatus reviewStatus = new ReviewStatus();
    reviewStatus.toRepeatCount = (int) getReviewPointsNeedToRepeat(0, timeZone).count();
    reviewStatus.learntCount = userModel.learntCount();
    reviewStatus.notLearntCount = notLearntCount();
    reviewStatus.toInitialReviewCount = toInitialReviewCount(timeZone);

    return reviewStatus;
  }
}
