package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AssimilationCountDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ReviewScope;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

public class AssimilationService {
  private final UserModel userModel;
  private final ModelFactoryService modelFactoryService;
  private final SubscriptionService subscriptionService;
  private final Timestamp currentUTCTimestamp;
  private final ZoneId timeZone;

  public AssimilationService(
      UserModel user,
      ModelFactoryService modelFactoryService,
      SubscriptionService subscriptionService,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone) {
    userModel = user;
    this.modelFactoryService = modelFactoryService;
    this.subscriptionService = subscriptionService;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.timeZone = timeZone;
  }

  private Stream<Note> getDueNoteToAssimilate(ReviewScope reviewScope, int count) {
    return reviewScope.getUnassimilatedNotes().limit(count);
  }

  private Stream<Note> getDueNoteToAssimilate(Stream<Note> notes, int count) {
    return notes.limit(count);
  }

  private Stream<Subscription> getSubscriptionStream() {
    return userModel.getEntity().getSubscriptions().stream();
  }

  public Stream<Note> getNotesToAssimilate() {
    int remainingDailyCount = getRemainingDailyAssimilationCount();
    if (remainingDailyCount <= 0) {
      return Stream.empty();
    }

    List<Integer> assimilatedNoteIdsForToday =
        getNotesAssimilatedToday().stream().map(MemoryTracker::getNote).map(Note::getId).toList();

    return Stream.concat(
            getDueNoteFromSubscription(assimilatedNoteIdsForToday),
            getDueNoteToAssimilate(userModel, remainingDailyCount))
        .limit(remainingDailyCount);
  }

  private Stream<Note> getDueNoteFromSubscription(List<Integer> todaysReviewedNoteIds) {
    return getSubscriptionStream()
        .flatMap(
            sub ->
                getDueNoteToAssimilate(
                    subscriptionService.getUnassimilatedNotes(sub),
                    subscriptionService.needToLearnCountToday(sub, todaysReviewedNoteIds)));
  }

  private int getRemainingDailyAssimilationCount() {
    return userModel.getEntity().getDailyAssimilationCount() - getAssimilatedCountOfTheDay();
  }

  public AssimilationCountDTO getCounts() {
    AssimilationCounter counter =
        new AssimilationCounter(
            calculateSubscribedCount(),
            userModel.getUnassimilatedNoteCount(),
            getAssimilatedCountOfTheDay(),
            userModel.getEntity().getDailyAssimilationCount());
    return counter.toDTO();
  }

  private int calculateSubscribedCount() {
    return getSubscriptionStream().mapToInt(subscriptionService::getUnassimilatedNoteCount).sum();
  }

  private int getAssimilatedCountOfTheDay() {
    return getNotesAssimilatedToday().size();
  }

  private List<MemoryTracker> getNotesAssimilatedToday() {
    Timestamp oneDayAgo = TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, -24);
    return userModel.getRecentMemoryTrackers(oneDayAgo).stream()
        .filter(
            p ->
                TimestampOperations.getDayId(p.getAssimilatedAt(), timeZone)
                    == TimestampOperations.getDayId(currentUTCTimestamp, timeZone))
        .filter(p -> !p.getRemovedFromTracking())
        .toList();
  }
}
