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
  private final List<AssimilationUnitSource> unitSources;
  private final Timestamp currentUTCTimestamp;
  private final ZoneId timeZone;

  public AssimilationService(
      User user,
      UserService userService,
      SubscriptionService subscriptionService,
      List<AssimilationUnitSource> unitSources,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone) {
    this.user = user;
    this.userService = userService;
    this.subscriptionService = subscriptionService;
    this.unitSources = unitSources;
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
    return allCandidateUnits().sorted(AssimilationUnit.ORDER).findFirst();
  }

  private Stream<AssimilationUnit> allCandidateUnits() {
    return Stream.of(subscriptionUnits(), ownedUnits()).flatMap(stream -> stream);
  }

  private Stream<AssimilationUnit> ownedUnits() {
    return unitSources.stream().flatMap(source -> source.streamForUser(user));
  }

  private Stream<AssimilationUnit> subscriptionUnits() {
    List<Integer> todaysAssimilatedNoteIds = assimilatedNoteIdsForToday();
    return getSubscriptionStream()
        .flatMap(
            sub -> {
              int budget = subscriptionService.needToLearnCountToday(sub, todaysAssimilatedNoteIds);
              Stream<AssimilationUnit> candidates =
                  unitSources.stream().flatMap(source -> source.streamForSubscription(sub));
              return candidates.sorted(AssimilationUnit.ORDER).limit(budget);
            });
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
    return unitSources.stream().mapToInt(source -> source.countForSubscription(subscription)).sum();
  }

  private int calculateOwnedUnitCount() {
    return unitSources.stream().mapToInt(source -> source.countForUser(user)).sum();
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
