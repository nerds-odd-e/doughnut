package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AssimilationCountDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

public class AssimilationService {
  private final User user;
  private final SubscriptionService subscriptionService;
  private final UserService userService;
  private final Timestamp currentUTCTimestamp;
  private final ZoneId timeZone;

  public AssimilationService(
      User user,
      SubscriptionService subscriptionService,
      UserService userService,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone) {
    this.user = user;
    this.subscriptionService = subscriptionService;
    this.userService = userService;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.timeZone = timeZone;
  }

  private Stream<Note> getDueNoteToAssimilate(Stream<Note> notes, int count) {
    return notes.limit(count);
  }

  private Stream<Subscription> getSubscriptionStream() {
    return user.getSubscriptions().stream();
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
            getDueNoteToAssimilate(userService.getUnassimilatedNotes(user), remainingDailyCount))
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
    return user.getDailyAssimilationCount() - getAssimilatedCountOfTheDay();
  }

  public AssimilationCountDTO getCounts() {
    AssimilationCounter counter =
        new AssimilationCounter(
            calculateSubscribedCount(),
            userService.getUnassimilatedNoteCount(user),
            getAssimilatedCountOfTheDay(),
            user.getDailyAssimilationCount());
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
    return userService.getRecentMemoryTrackers(user, oneDayAgo).stream()
        .filter(
            p ->
                TimestampOperations.getDayId(p.getAssimilatedAt(), timeZone)
                    == TimestampOperations.getDayId(currentUTCTimestamp, timeZone))
        .filter(p -> !p.getRemovedFromTracking())
        .toList();
  }
}
