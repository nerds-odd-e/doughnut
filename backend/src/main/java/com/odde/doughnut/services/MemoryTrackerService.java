package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
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

    memoryTracker.setUser(currentUser);
    memoryTracker.setAssimilatedAt(currentTime);
    memoryTracker.setLastRecalledAt(currentTime);
    updateForgettingCurve(memoryTracker, 0);
    return memoryTracker;
  }

  public void updateForgettingCurve(MemoryTracker memoryTracker, int adjustment) {
    memoryTracker.setForgettingCurveIndex(memoryTracker.getForgettingCurveIndex() + adjustment);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    modelFactoryService.save(memoryTracker);
  }

  public void updateMemoryTrackerAfterAnsweringQuestion(
      User user, Timestamp currentUTCTimestamp, Boolean correct, RecallPrompt recallPrompt) {
    MemoryTracker memoryTracker =
        new UserModel(user, modelFactoryService)
            .getMemoryTrackerFor(recallPrompt.getPredefinedQuestion().getNote());
    if (memoryTracker != null) {
      markAsRepeated(currentUTCTimestamp, correct, memoryTracker);
    }
  }

  public void markAsRepeated(
      Timestamp currentUTCTimestamp, Boolean correct, MemoryTracker memoryTracker) {
    memoryTracker.markAsRepeated(currentUTCTimestamp, correct);
    modelFactoryService.save(memoryTracker);
  }
}
