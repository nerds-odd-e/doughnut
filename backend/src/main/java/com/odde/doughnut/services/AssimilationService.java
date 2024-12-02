package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AssimilationCountDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ReviewScope;
import com.odde.doughnut.models.SubscriptionModel;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

public class AssimilationService {
  private final UserModel userModel;
  private final ModelFactoryService modelFactoryService;
  private final Timestamp currentUTCTimestamp;
  private final ZoneId timeZone;

  public AssimilationService(
      UserModel user,
      ModelFactoryService modelFactoryService,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone) {
    userModel = user;
    this.modelFactoryService = modelFactoryService;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.timeZone = timeZone;
  }

  private Stream<Note> getDueNewMemoryTracker(ReviewScope reviewScope, int count) {
    return reviewScope.getThingHaveNotBeenReviewedAtAll().limit(count);
  }

  private int getPendingNewMemoryTrackerCount(ReviewScope reviewScope) {
    return reviewScope.getThingsHaveNotBeenReviewedAtAllCount();
  }

  private Stream<SubscriptionModel> getSubscriptionModelStream() {
    return userModel.getEntity().getSubscriptions().stream()
        .map(modelFactoryService::toSubscriptionModel);
  }

  public Stream<Note> getDueInitialMemoryTrackers() {
    int remainingDailyCount = getRemainingDailyNewNotesCount();
    if (remainingDailyCount <= 0) {
      return Stream.empty();
    }

    List<Integer> todaysReviewedNoteIds = getTodaysReviewedNoteIds();
    return Stream.concat(
            getSubscriptionDueNotes(todaysReviewedNoteIds),
            getDueNewMemoryTracker(userModel, remainingDailyCount))
        .limit(remainingDailyCount);
  }

  private Stream<Note> getSubscriptionDueNotes(List<Integer> todaysReviewedNoteIds) {
    return getSubscriptionModelStream()
        .flatMap(sub -> getDueNewMemoryTracker(
            sub,
            sub.needToLearnCountToday(todaysReviewedNoteIds)
        ));
  }

  private List<Integer> getTodaysReviewedNoteIds() {
    return getNewMemoryTrackersOfToday().stream()
        .map(MemoryTracker::getNote)
        .map(Note::getId)
        .toList();
  }

  private int getRemainingDailyNewNotesCount() {
    return userModel.getEntity().getDailyNewNotesCount() - getAssimilatedCountOfTheDay();
  }

  public AssimilationCountDTO getCounts() {
    AssimilationCounter counter = new AssimilationCounter(
        calculateSubscribedCount(),
        getPendingNewMemoryTrackerCount(userModel),
        getAssimilatedCountOfTheDay(),
        userModel.getEntity().getDailyNewNotesCount()
    );
    return counter.toDTO();
  }

  private int calculateSubscribedCount() {
    return getSubscriptionModelStream()
        .mapToInt(this::getPendingNewMemoryTrackerCount)
        .sum();
  }

  private int getAssimilatedCountOfTheDay() {
    return getNewMemoryTrackersOfToday().size();
  }

  private List<MemoryTracker> getNewMemoryTrackersOfToday() {
    Timestamp oneDayAgo = TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, -24);
    return userModel.getRecentMemoryTrackers(oneDayAgo).stream()
        .filter(p -> TimestampOperations.getDayId(p.getAssimilatedAt(), timeZone)
            == TimestampOperations.getDayId(currentUTCTimestamp, timeZone))
        .filter(p -> !p.getRemovedFromTracking())
        .toList();
  }
}
