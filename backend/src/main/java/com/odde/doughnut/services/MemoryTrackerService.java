package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.MemoryTrackerModel;
import java.sql.Timestamp;

public class MemoryTrackerService {
  private final ModelFactoryService modelFactoryService;

  public MemoryTrackerService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public MemoryTracker assimilate(
      InitialInfo initialInfo, User currentUser, Timestamp currentTime) {
    MemoryTracker memoryTracker =
        MemoryTracker.buildMemoryTrackerForNote(
            modelFactoryService.entityManager.find(Note.class, initialInfo.noteId));
    memoryTracker.setRemovedFromTracking(initialInfo.skipMemoryTracking);

    MemoryTrackerModel memoryTrackerModel = modelFactoryService.toMemoryTrackerModel(memoryTracker);
    memoryTrackerModel.assimilate(currentTime, currentUser);
    return memoryTrackerModel.getEntity();
  }
}
