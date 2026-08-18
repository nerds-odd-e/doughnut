package com.odde.doughnut.testability;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.ProductOutcome;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.MemoryTrackerService;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"e2e", "test"})
class SeedGradedMemoryTrackerWorker {
  @Autowired EntityManager entityManager;
  @Autowired EntityPersister entityPersister;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired MemoryTrackerService memoryTrackerService;

  String seed(int memoryTrackerId, float stability, float difficulty) {
    MemoryTracker tracker = entityManager.find(MemoryTracker.class, memoryTrackerId);
    if (tracker == null) {
      throw new IllegalArgumentException("No memory tracker with id " + memoryTrackerId);
    }
    tracker.setStability(stability);
    tracker.setDifficulty(difficulty);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    tracker.setLastRecalledAt(now);
    tracker.setNextRecallAt(tracker.calculateNextRecallAt());
    memoryTrackerService.persistRecallLog(tracker, now, ProductOutcome.GOOD, null);
    entityPersister.save(tracker);
    return "OK";
  }
}
