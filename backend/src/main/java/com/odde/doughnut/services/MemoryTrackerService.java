package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.MemoryTrackerModel;
import com.odde.doughnut.testability.TestabilitySettings;

public class MemoryTrackerService {
  private final ModelFactoryService modelFactoryService;
  private final TestabilitySettings testabilitySettings;

  public MemoryTrackerService(
      ModelFactoryService modelFactoryService, TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.testabilitySettings = testabilitySettings;
  }

  public MemoryTracker assimilate(InitialInfo initialInfo, User currentUser) {
    MemoryTracker memoryTracker =
        MemoryTracker.buildMemoryTrackerForNote(
            modelFactoryService.entityManager.find(Note.class, initialInfo.noteId));
    memoryTracker.setRemovedFromTracking(initialInfo.skipMemoryTracking);

    MemoryTrackerModel memoryTrackerModel = modelFactoryService.toMemoryTrackerModel(memoryTracker);
    memoryTrackerModel.assimilate(testabilitySettings.getCurrentUTCTimestamp(), currentUser);
    return memoryTrackerModel.getEntity();
  }
}
