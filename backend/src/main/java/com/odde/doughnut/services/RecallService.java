package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.ReviewStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

public class RecallService extends OnboardingService {

  public RecallService(
      UserModel user,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone,
      ModelFactoryService modelFactoryService) {
    super(user, modelFactoryService, currentUTCTimestamp, timeZone);
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

  public ReviewStatus getReviewStatus() {
    ReviewStatus reviewStatus = new ReviewStatus();
    reviewStatus.toRepeatCount = (int) getMemoryTrackersNeedToRepeat(0).count();
    reviewStatus.learntCount = learntCount();
    reviewStatus.notLearntCount = notLearntCount();
    reviewStatus.toInitialReviewCount = toInitialReviewCount();

    return reviewStatus;
  }
}
