package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.MemoryTrackerLite;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecallService {

  private final UserService userService;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final RecallPromptRepository recallPromptRepository;

  @Autowired
  public RecallService(
      UserService userService,
      MemoryTrackerRepository memoryTrackerRepository,
      RecallPromptRepository recallPromptRepository) {
    this.userService = userService;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.recallPromptRepository = recallPromptRepository;
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

  public DueMemoryTrackers getDueMemoryTrackers(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone, int dueInDays) {
    List<MemoryTrackerLite> toRepeat =
        getMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, dueInDays)
            .map(
                mt -> {
                  MemoryTrackerLite lite = new MemoryTrackerLite();
                  lite.setMemoryTrackerId(mt.getId());
                  lite.setSpelling(mt.getSpelling() != null && mt.getSpelling());
                  return lite;
                })
            .toList();
    DueMemoryTrackers dueMemoryTrackers = new DueMemoryTrackers();
    dueMemoryTrackers.setDueInDays(dueInDays);
    dueMemoryTrackers.setToRepeat(toRepeat);

    // Set recall status
    dueMemoryTrackers.totalAssimilatedCount = totalAssimilatedCount(user);
    dueMemoryTrackers.setCurrentRecallWindowEndAt(
        TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone));

    return dueMemoryTrackers;
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
