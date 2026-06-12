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
import java.util.Optional;
import java.util.stream.Stream;

public class AssimilationService {
  private final User user;
  private final UserService userService;
  private final SubscriptionService subscriptionService;
  private final UnassimilatedPropertyService unassimilatedPropertyService;
  private final Timestamp currentUTCTimestamp;
  private final ZoneId timeZone;

  public AssimilationService(
      User user,
      UserService userService,
      SubscriptionService subscriptionService,
      UnassimilatedPropertyService unassimilatedPropertyService,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone) {
    this.user = user;
    this.userService = userService;
    this.subscriptionService = subscriptionService;
    this.unassimilatedPropertyService = unassimilatedPropertyService;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.timeZone = timeZone;
  }

  private Stream<Subscription> getSubscriptionStream() {
    return user.getSubscriptions().stream();
  }

  public Optional<Note> getNextNoteToAssimilate() {
    return getNextAssimilationUnit().map(AssimilationUnit::note);
  }

  public Optional<AssimilationUnit> getNextAssimilationUnit() {
    int remainingDailyCount = getRemainingDailyAssimilationCount();
    if (remainingDailyCount > 0) {
      return unitsEligibleToAssimilate(remainingDailyCount).findFirst();
    }
    return unitsEligiblePastUserDailyCap().findFirst();
  }

  private Stream<AssimilationUnit> unitsEligibleToAssimilate(int remainingDailyCount) {
    return allCandidateUnits().sorted(AssimilationUnit.ORDER).limit(remainingDailyCount);
  }

  /** Subscription daily limits still apply; only the user's daily cap is ignored. */
  private Stream<AssimilationUnit> unitsEligiblePastUserDailyCap() {
    return allCandidateUnits().sorted(AssimilationUnit.ORDER);
  }

  private Stream<AssimilationUnit> allCandidateUnits() {
    return Stream.of(
            subscriptionUnits(),
            userService.getUnassimilatedNotes(user).map(AssimilationUnit::forNote),
            unassimilatedPropertyService.streamUnassimilatedPropertiesForUser(user))
        .flatMap(stream -> stream);
  }

  private Stream<AssimilationUnit> subscriptionUnits() {
    List<Integer> todaysAssimilatedNoteIds = assimilatedNoteIdsForToday();
    return getSubscriptionStream()
        .flatMap(
            sub -> {
              int budget = subscriptionService.needToLearnCountToday(sub, todaysAssimilatedNoteIds);
              Stream<AssimilationUnit> candidates =
                  Stream.concat(
                      subscriptionService.getUnassimilatedNotes(sub).map(AssimilationUnit::forNote),
                      unassimilatedPropertyService.streamUnassimilatedPropertiesForNotebook(
                          user, sub.getNotebook().getId()));
              return candidates.sorted(AssimilationUnit.ORDER).limit(budget);
            });
  }

  private List<Integer> assimilatedNoteIdsForToday() {
    return getNotesAssimilatedToday().stream()
        .map(MemoryTracker::getNote)
        .map(Note::getId)
        .toList();
  }

  private int getRemainingDailyAssimilationCount() {
    return user.getDailyAssimilationCount() - getAssimilatedCountOfTheDay();
  }

  public AssimilationCountDTO getCounts() {
    AssimilationCounter counter =
        new AssimilationCounter(
            calculateSubscribedCount(),
            userService.getUnassimilatedNoteCount(user)
                + unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user),
            getAssimilatedCountOfTheDay(),
            user.getDailyAssimilationCount());
    return counter.toDTO();
  }

  private int calculateSubscribedCount() {
    return getSubscriptionStream()
        .mapToInt(
            sub ->
                subscriptionService.getUnassimilatedNoteCount(sub)
                    + unassimilatedPropertyService.countUnassimilatedPropertiesForNotebook(
                        user, sub.getNotebook().getId()))
        .sum();
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
