package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class MemoryTrackerBuilder extends EntityBuilder<MemoryTracker> {
  private int recallLogsToCreate = 0;

  public MemoryTrackerBuilder(MemoryTracker memoryTracker, MakeMe makeMe) {
    super(makeMe, memoryTracker);
    assimilatedAt(makeMe.aTimestamp().of(0, 0).please());
  }

  public MemoryTrackerBuilder by(User user) {
    entity.setUser(user);
    return this;
  }

  public MemoryTrackerBuilder assimilatedAt(Timestamp assimilatedTimestamp) {
    entity.setAssimilatedAt(assimilatedTimestamp);
    entity.setNextRecallAt(assimilatedTimestamp);
    return this;
  }

  public MemoryTrackerBuilder lastRecalledAt(Timestamp lastRecalledAt) {
    entity.setLastRecalledAt(lastRecalledAt);
    return this;
  }

  public MemoryTrackerBuilder afterNthStrictRecall(Integer recallDone) {
    for (int i = 0; i < recallDone; i++) {
      entity.recalledSuccessfully(entity.getNextRecallAt());
    }
    return this;
  }

  public MemoryTrackerBuilder recallCount(int recallCount) {
    this.recallLogsToCreate = recallCount;
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  @Override
  protected void afterCreate(boolean needPersist) {
    for (int i = 0; i < recallLogsToCreate; i++) {
      RecallLog log =
          makeMe.aRecallLogFor(entity).productOutcome(ProductOutcome.GOOD).please(needPersist);
      entity.addRecallLog(log);
    }
  }

  public MemoryTrackerBuilder stabilityAndNextRecallAt(float value) {
    entity.setStability(value);
    if (!entity.isNew() && entity.getLastRecalledAt() == null) {
      entity.setLastRecalledAt(entity.getAssimilatedAt());
    }
    entity.setNextRecallAt(entity.calculateNextRecallAt());
    return this;
  }

  public MemoryTrackerBuilder difficulty(float difficulty) {
    entity.setDifficulty(difficulty);
    return this;
  }

  public MemoryTrackerBuilder removedFromTracking() {
    entity.setRemovedFromTracking(true);
    return this;
  }

  public MemoryTrackerBuilder nextRecallAt(Timestamp timestamp) {
    entity.setNextRecallAt(timestamp);
    return this;
  }

  public MemoryTrackerBuilder spelling() {
    entity.setType(MemoryTrackerType.SPELLING);
    return this;
  }

  public MemoryTrackerBuilder commissioned() {
    entity.setType(MemoryTrackerType.COMMISSIONED);
    return this;
  }

  public MemoryTrackerBuilder propertyKey(String propertyKey) {
    entity.setPropertyKey(propertyKey);
    return this;
  }

  public MemoryTrackerBuilder deletedAt(Timestamp deletedAt) {
    entity.setDeletedAt(deletedAt);
    return this;
  }
}
