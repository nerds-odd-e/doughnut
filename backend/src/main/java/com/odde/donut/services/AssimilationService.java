package com.odde.donut.services;

import com.odde.donut.controllers.dto.AssimilationCountDTO;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Subscription;
import com.odde.donut.entities.User;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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

  /** Notes first: the best note bounds how far each gated property scan goes. */
  public Optional<AssimilationUnit> getNextAssimilationUnit() {
    List<Subscription> subscriptions = subscriptionsWithinBudget();
    Optional<AssimilationUnit> best =
        bestOf(Optional.empty(), userService.getUnassimilatedNotes(user));
    for (Subscription subscription : subscriptions) {
      best = bestOf(best, subscriptionService.getUnassimilatedNotes(subscription));
    }
    best =
        bestOf(
            best,
            unassimilatedPropertyService.streamUnassimilatedPropertiesForUser(
                user, precedes(best)));
    for (Subscription subscription : subscriptions) {
      best =
          bestOf(
              best,
              unassimilatedPropertyService.streamUnassimilatedPropertiesForSubscription(
                  subscription, precedes(best)));
    }
    return best;
  }

  /** {@code best}, or the head of the ordered {@code candidates} when that precedes it. */
  private static Optional<AssimilationUnit> bestOf(
      Optional<AssimilationUnit> best, Stream<AssimilationUnit> candidates) {
    try (candidates) {
      return candidates.findFirst().filter(precedes(best)).or(() -> best);
    }
  }

  private static Predicate<AssimilationUnit> precedes(Optional<AssimilationUnit> best) {
    return unit -> best.map(b -> AssimilationUnit.ORDER.compare(unit, b) < 0).orElse(true);
  }

  private List<Subscription> subscriptionsWithinBudget() {
    List<Integer> todaysAssimilatedNoteIds = assimilatedNoteIdsForToday();
    return getSubscriptionStream()
        .filter(
            sub ->
                subscriptionService.remainingDailyAssimilationTarget(sub, todaysAssimilatedNoteIds)
                    > 0)
        .toList();
  }

  private List<Integer> assimilatedNoteIdsForToday() {
    return getNotesAssimilatedToday().stream()
        .map(MemoryTracker::getNote)
        .map(Note::getId)
        .toList();
  }

  public AssimilationCountDTO getCounts() {
    AssimilationCounter counter =
        new AssimilationCounter(
            calculateSubscribedUnitCount(),
            calculateOwnedUnitCount(),
            getAssimilatedCountOfTheDay(),
            user.getDailyAssimilationCount());
    return counter.toDTO();
  }

  private int calculateSubscribedUnitCount() {
    return getSubscriptionStream().mapToInt(this::subscribedUnitCount).sum();
  }

  private int subscribedUnitCount(Subscription subscription) {
    return subscriptionService.getUnassimilatedNoteCount(subscription)
        + unassimilatedPropertyService.countUnassimilatedPropertiesForSubscription(subscription);
  }

  private int calculateOwnedUnitCount() {
    return userService.getUnassimilatedNoteCount(user)
        + unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user);
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
