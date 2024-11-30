package com.odde.doughnut.models;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.ReviewStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

public class Reviewing {
  private final UserModel userModel;
  private final Timestamp currentUTCTimestamp;
  private final ZoneId timeZone;
  private final ModelFactoryService modelFactoryService;

  public Reviewing(
      UserModel user,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone,
      ModelFactoryService modelFactoryService) {
    userModel = user;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.timeZone = timeZone;
    this.modelFactoryService = modelFactoryService;
  }

  public Stream<Note> getDueInitialMemoryTrackers() {
    int count = remainingDailyNewNotesCount();
    if (count == 0) {
      return Stream.empty();
    }
    List<Integer> alreadyInitialReviewed =
        getNewMemoryTrackersOfToday().stream()
            .map(MemoryTracker::getNote)
            .map(Note::getId)
            .toList();
    return Stream.concat(
            getSubscriptionModelStream()
                .flatMap(
                    sub ->
                        getDueNewMemoryTracker(
                            sub, sub.needToLearnCountToday(alreadyInitialReviewed))),
            getDueNewMemoryTracker(userModel, count))
        .limit(count);
  }

  private Stream<Note> getDueNewMemoryTracker(ReviewScope reviewScope, int count) {
    return reviewScope.getThingHaveNotBeenReviewedAtAll().limit(count);
  }

  private Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(int dueInDays) {
    return userModel.getMemoryTrackerNeedToRepeat(
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24), timeZone);
  }

  private int notLearntCount() {
    Integer subscribedCount =
        getSubscriptionModelStream()
            .map(this::getPendingNewMemoryTrackerCount)
            .reduce(Integer::sum)
            .orElse(0);
    return subscribedCount + getPendingNewMemoryTrackerCount(userModel);
  }

  private int getPendingNewMemoryTrackerCount(ReviewScope reviewScope) {
    return reviewScope.getThingsHaveNotBeenReviewedAtAllCount();
  }

  private int toInitialReviewCount() {
    if (getDueInitialMemoryTrackers().findFirst().isEmpty()) {
      return 0;
    }
    return Math.min(remainingDailyNewNotesCount(), notLearntCount());
  }

  private int remainingDailyNewNotesCount() {
    long sameDayCount = getNewMemoryTrackersOfToday().size();
    return (int) (userModel.entity.getDailyNewNotesCount() - sameDayCount);
  }

  private List<MemoryTracker> getNewMemoryTrackersOfToday() {
    Timestamp oneDayAgo = TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, -24);
    return userModel.getRecentMemoryTrackers(oneDayAgo).stream()
        .filter(p -> userModel.isInitialReviewOnSameDay(p, currentUTCTimestamp, timeZone))
        .filter(p -> !p.getRemovedFromReview())
        .toList();
  }

  private Stream<SubscriptionModel> getSubscriptionModelStream() {
    return userModel.entity.getSubscriptions().stream()
        .map(modelFactoryService::toSubscriptionModel);
  }

  public DueMemoryTrackers getDueMemoryTrackers(int dueInDays) {
    List<Integer> toRepeat =
        getMemoryTrackersNeedToRepeat(dueInDays).map(MemoryTracker::getId).toList();
    DueMemoryTrackers dueMemoryTrackers = new DueMemoryTrackers();
    dueMemoryTrackers.setDueInDays(dueInDays);
    dueMemoryTrackers.setToRepeat(toRepeat);
    return dueMemoryTrackers;
  }

  public ReviewStatus getReviewStatus() {
    ReviewStatus reviewStatus = new ReviewStatus();
    reviewStatus.toRepeatCount = (int) getMemoryTrackersNeedToRepeat(0).count();
    reviewStatus.learntCount = userModel.learntCount();
    reviewStatus.notLearntCount = notLearntCount();
    reviewStatus.toInitialReviewCount = toInitialReviewCount();

    return reviewStatus;
  }
}
