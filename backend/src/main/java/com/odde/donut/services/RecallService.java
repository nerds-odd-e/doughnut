package com.odde.donut.services;

import com.odde.donut.controllers.dto.DueCommissionedMemoryTrackerLite;
import com.odde.donut.controllers.dto.DueMemoryTrackers;
import com.odde.donut.controllers.dto.MemoryTrackerLite;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.utils.Randomizer;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecallService {

  private final UserService userService;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final TestabilitySettings testabilitySettings;

  @Autowired
  public RecallService(
      UserService userService,
      MemoryTrackerRepository memoryTrackerRepository,
      RecallPromptRepository recallPromptRepository,
      TestabilitySettings testabilitySettings) {
    this.userService = userService;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.testabilitySettings = testabilitySettings;
  }

  private int totalAssimilatedCount(User user) {
    return memoryTrackerRepository.countByUserNotRemoved(user.getId());
  }

  private Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone, int dueInDays) {
    return userService.getMemoryTrackersNeedToRepeat(
        user,
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24),
        timeZone);
  }

  private Stream<MemoryTracker> getCommissionedMemoryTrackersNeedToRepeat(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone, int dueInDays) {
    return userService.getCommissionedMemoryTrackersNeedToRepeat(
        user,
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24),
        timeZone);
  }

  public DueMemoryTrackers getDueMemoryTrackers(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone, int dueInDays) {
    List<MemoryTrackerLite> toRepeat =
        orderSharedDueQueue(
                getMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, dueInDays)
                    .toList())
            .stream()
            .map(MemoryTrackerLite::from)
            .toList();
    List<DueCommissionedMemoryTrackerLite> dueCommissioned =
        getCommissionedMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, dueInDays)
            .map(
                mt -> {
                  DueCommissionedMemoryTrackerLite lite = new DueCommissionedMemoryTrackerLite();
                  lite.setMemoryTrackerId(mt.getId());
                  Notebook notebook = mt.getNote().getNotebook();
                  lite.setNotebookId(notebook.getId());
                  lite.setNotebookName(notebook.getName());
                  return lite;
                })
            .toList();
    DueMemoryTrackers dueMemoryTrackers = new DueMemoryTrackers();
    dueMemoryTrackers.setDueInDays(dueInDays);
    dueMemoryTrackers.setToRepeat(toRepeat);
    dueMemoryTrackers.setDueCommissioned(dueCommissioned);

    // Set recall status
    dueMemoryTrackers.totalAssimilatedCount = totalAssimilatedCount(user);
    dueMemoryTrackers.setCurrentRecallWindowEndAt(
        TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone));

    return dueMemoryTrackers;
  }

  private List<MemoryTracker> orderSharedDueQueue(List<MemoryTracker> trackers) {
    Map<Long, List<MemoryTracker>> byBand = new TreeMap<>();
    for (MemoryTracker tracker : trackers) {
      byBand
          .computeIfAbsent(twelveHourUtcBand(tracker.getNextRecallAt()), band -> new ArrayList<>())
          .add(tracker);
    }
    Randomizer randomizer = testabilitySettings.getRandomizer();
    List<MemoryTracker> ordered = new ArrayList<>(trackers.size());
    for (List<MemoryTracker> inBand : byBand.values()) {
      List<MemoryTracker> spelling = new ArrayList<>();
      List<MemoryTracker> others = new ArrayList<>();
      for (MemoryTracker tracker : inBand) {
        (tracker.isSpelling() ? spelling : others).add(tracker);
      }
      ordered.addAll(spelling);
      ordered.addAll(randomizer.shuffle(others));
    }
    return ordered;
  }

  private static long twelveHourUtcBand(Timestamp nextRecallAt) {
    long epochHours = nextRecallAt.toInstant().getEpochSecond() / 3600;
    return Math.floorDiv(epochHours, 12);
  }

  public int getToRecallCount(User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    return (int) getMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, 0).count();
  }

  public List<RecallPrompt> getPreviouslyAnsweredRecallPrompts(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    Timestamp startTime = TimestampOperations.startOfHalfADay(currentUTCTimestamp, timeZone);
    Timestamp endTime = TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone);

    return recallPromptRepository.findAnsweredRecallPromptsInTimeRange(
        user.getId(), startTime, endTime);
  }
}
