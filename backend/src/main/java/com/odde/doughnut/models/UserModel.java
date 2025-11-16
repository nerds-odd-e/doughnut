package com.odde.doughnut.models;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.TimestampService;
import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;

public class UserModel implements ReviewScope {

  @Getter protected final User entity;
  protected final ModelFactoryService modelFactoryService;
  private final TimestampService timestampService;

  public UserModel(
      User user, ModelFactoryService modelFactoryService, TimestampService timestampService) {
    this.entity = user;
    this.modelFactoryService = modelFactoryService;
    this.timestampService = timestampService;
  }

  private Authorization getAuthorization() {
    return modelFactoryService.toAuthorization(entity);
  }

  public String getName() {
    return entity.getName();
  }

  public void setAndSaveDailyAssimilationCount(Integer dailyAssimilationCount) {
    entity.setDailyAssimilationCount(dailyAssimilationCount);
    modelFactoryService.save(entity);
  }

  public void setAndSaveSpaceIntervals(String spaceIntervals) {
    entity.setSpaceIntervals(spaceIntervals);
    modelFactoryService.save(entity);
  }

  public int getUnassimilatedNoteCount() {
    return modelFactoryService.noteReviewRepository.countByOwnershipWhereThereIsNoMemoryTracker(
        entity.getId(), entity.getOwnership().getId());
  }

  @Override
  public Stream<Note> getUnassimilatedNotes() {
    return modelFactoryService.noteReviewRepository.findByOwnershipWhereThereIsNoMemoryTracker(
        entity.getId(), entity.getOwnership().getId());
  }

  public List<MemoryTracker> getRecentMemoryTrackers(Timestamp since) {
    return modelFactoryService.memoryTrackerRepository.findAllByUserAndAssimilatedAtGreaterThan(
        entity, since);
  }

  public Stream<MemoryTracker> getMemoryTrackerNeedToRepeat(
      Timestamp currentUTCTimestamp, ZoneId timeZone) {
    final Timestamp timestamp = timestampService.alignByHalfADay(currentUTCTimestamp, timeZone);
    return modelFactoryService.memoryTrackerRepository
        .findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(entity.getId(), timestamp);
  }

  public List<MemoryTracker> getMemoryTrackersFor(Note note) {
    if (entity == null) return List.of();

    return modelFactoryService.memoryTrackerRepository.findByUserAndNote(
        entity.getId(), note.getId());
  }

  public <T> void assertAuthorization(T object) throws UnexpectedNoAccessRightException {
    getAuthorization().assertAuthorization(object);
  }

  public <T> void assertReadAuthorization(T object) throws UnexpectedNoAccessRightException {
    getAuthorization().assertReadAuthorization(object);
  }

  public void assertAdminAuthorization() throws UnexpectedNoAccessRightException {
    getAuthorization().assertAdminAuthorization();
  }

  public void assertLoggedIn() {
    getAuthorization().assertLoggedIn();
  }
}
