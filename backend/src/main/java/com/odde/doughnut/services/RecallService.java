package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.entities.MemoryTracker;
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

  public RecallService(UserModel userModel, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    this.userModel = userModel;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.timeZone = timeZone;
  }

  private Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(int dueInDays) {
    return userModel.getMemoryTrackerNeedToRepeat(
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24), timeZone);
  }

  public DueMemoryTrackers getDueMemoryTrackers(int dueInDays) {
    List<Integer> toRepeat =
        getMemoryTrackersNeedToRepeat(dueInDays).map(MemoryTracker::getId).toList();
    DueMemoryTrackers dueMemoryTrackers = new DueMemoryTrackers();
    dueMemoryTrackers.setDueInDays(dueInDays);
    dueMemoryTrackers.setToRepeat(toRepeat);
    return dueMemoryTrackers;
  }

  public int getToRecallCount() {
    return (int) getMemoryTrackersNeedToRepeat(0).count();
  }
}
