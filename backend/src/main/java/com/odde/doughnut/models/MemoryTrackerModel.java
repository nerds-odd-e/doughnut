package com.odde.doughnut.models;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public record MemoryTrackerModel(MemoryTracker entity, ModelFactoryService modelFactoryService) {

  public MemoryTracker getEntity() {
    return entity;
  }

  public void markAsRepeated(Timestamp currentUTCTimestamp, boolean successful) {
    entity.setRepetitionCount(entity.getRepetitionCount() + 1);
    if (successful) {
      entity.reviewedSuccessfully(currentUTCTimestamp);
    } else {
      entity.reviewFailed(currentUTCTimestamp);
    }
    this.modelFactoryService.save(entity);
  }
}
