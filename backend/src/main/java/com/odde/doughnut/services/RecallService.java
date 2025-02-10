package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.RecallStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

public class RecallService {

  private final UserModel userModel;
  private final Timestamp currentUTCTimestamp;
  private final ZoneId timeZone;
  private final ModelFactoryService modelFactoryService;

  public RecallService(
      UserModel userModel,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone,
      ModelFactoryService modelFactoryService) {
    this.userModel = userModel;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.timeZone = timeZone;
    this.modelFactoryService = modelFactoryService;
  }

  private int totalAssimilatedCount() {
    return modelFactoryService.memoryTrackerRepository.countByUserNotRemoved(
        userModel.getEntity().getId());
  }

  private Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(int dueInDays) {
    return userModel
        .getMemoryTrackerNeedToRepeat(
            TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24), timeZone)
        .filter(tracker -> !Boolean.TRUE.equals(tracker.getSpelling()));
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
    List<Integer> toRepeat =
        getMemoryTrackersNeedToRepeat(dueInDays).map(MemoryTracker::getId).toList();
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
