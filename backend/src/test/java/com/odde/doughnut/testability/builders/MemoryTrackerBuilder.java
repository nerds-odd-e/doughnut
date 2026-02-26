package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class MemoryTrackerBuilder extends EntityBuilder<MemoryTracker> {

  public MemoryTrackerBuilder(MemoryTracker memoryTracker, MakeMe makeMe) {
    super(makeMe, memoryTracker);
    assimilatedAt(makeMe.aTimestamp().of(0, 0).please());
  }

  public MemoryTrackerBuilder by(User user) {
    entity.setUser(user);
    return this;
  }

  public MemoryTrackerBuilder assimilatedAt(Timestamp reviewTimestamp) {
    entity.setAssimilatedAt(reviewTimestamp);
    entity.setLastRecalledAt(reviewTimestamp);
    entity.setNextRecallAt(reviewTimestamp);
    return this;
  }

  public MemoryTrackerBuilder afterNthStrictRepetition(Integer repetitionDone) {
    for (int i = 0; i < repetitionDone; i++) {
      entity.recalledSuccessfully(entity.getNextRecallAt(), null);
    }
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public MemoryTrackerBuilder forgettingCurveAndNextRecallAt(float value) {
    entity.setForgettingCurveIndex(value);
    entity.setNextRecallAt(entity.calculateNextRecallAt());
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
    entity.setSpelling(true);
    return this;
  }
}
