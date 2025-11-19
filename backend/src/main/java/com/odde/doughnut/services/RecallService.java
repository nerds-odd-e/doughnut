package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.MemoryTrackerLite;
import com.odde.doughnut.controllers.dto.RecallStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

public class RecallService {

  private final User user;
  private final Timestamp currentUTCTimestamp;
  private final ZoneId timeZone;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final UserService userService;

  public RecallService(
      User user,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone,
      MemoryTrackerRepository memoryTrackerRepository,
      UserService userService) {
    this.user = user;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.timeZone = timeZone;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.userService = userService;
  }

  private int totalAssimilatedCount() {
    return memoryTrackerRepository.countByUserNotRemoved(user.getId());
  }

  private Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(int dueInDays) {
    return userService.getMemoryTrackersNeedToRepeat(
        user,
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24),
        timeZone);
  }

  public RecallStatus getRecallStatus() {
    RecallStatus recallStatus = new RecallStatus();
    recallStatus.toRepeatCount = getToRecallCount();
    recallStatus.totalAssimilatedCount = totalAssimilatedCount();
    recallStatus.setRecallWindowEndAt(
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, 24));
    return recallStatus;
  }

  public DueMemoryTrackers getDueMemoryTrackers(int dueInDays) {
    List<MemoryTrackerLite> toRepeat =
        getMemoryTrackersNeedToRepeat(dueInDays)
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

    // Set recall status (always based on dueInDays=0)
    RecallStatus status = getRecallStatus();
    dueMemoryTrackers.toRepeatCount = status.toRepeatCount;
    dueMemoryTrackers.totalAssimilatedCount = status.totalAssimilatedCount;
    dueMemoryTrackers.setRecallWindowEndAt(status.getRecallWindowEndAt());

    return dueMemoryTrackers;
  }

  public int getToRecallCount() {
    return (int) getMemoryTrackersNeedToRepeat(0).count();
  }
}
