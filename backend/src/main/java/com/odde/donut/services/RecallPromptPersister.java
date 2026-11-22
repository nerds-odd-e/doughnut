package com.odde.donut.services;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.factoryServices.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Rechecks for a matching prompt inside the write transaction before saving a new one.
@Service
public class RecallPromptPersister {
  private final RecallPromptRepository recallPromptRepository;
  private final EntityPersister entityPersister;
  private final MemoryTrackerLifecycle memoryTrackerLifecycle;

  @Autowired
  public RecallPromptPersister(
      RecallPromptRepository recallPromptRepository,
      EntityPersister entityPersister,
      MemoryTrackerLifecycle memoryTrackerLifecycle) {
    this.recallPromptRepository = recallPromptRepository;
    this.entityPersister = entityPersister;
    this.memoryTrackerLifecycle = memoryTrackerLifecycle;
  }

  @Transactional
  public RecallPrompt persistRecallPromptForMcq(Mcq mcq, MemoryTracker memoryTracker) {
    Note currentFocus = entityPersister.find(Note.class, memoryTracker.getNote().getId());
    if (currentFocus == null) {
      return null;
    }
    String currentFingerprint = FocusNoteFingerprint.of(currentFocus);
    RecallPrompt existingPrompt =
        recallPromptRepository
            .findUnansweredMcqForFocus(memoryTracker.getId(), currentFingerprint)
            .orElse(null);
    if (existingPrompt != null) {
      return existingPrompt;
    }
    if (!currentFingerprint.equals(mcq.getFocusNoteFingerprint())) {
      return null;
    }
    return persistNewRecallPromptForMcq(mcq, memoryTracker);
  }

  @Transactional
  public RecallPrompt persistNewRecallPromptForMcq(Mcq mcq, MemoryTracker memoryTracker) {
    entityPersister.save(mcq);
    return entityPersister.save(RecallPrompt.forMcq(mcq, memoryTracker));
  }

  // A current imported question replaces the tracker's stale waiting prompts.
  @Transactional
  public void persistImportedRecallPromptForMcq(Mcq mcq, MemoryTracker memoryTracker) {
    persistNewRecallPromptForMcq(mcq, memoryTracker);
    String currentFingerprint = FocusNoteFingerprint.of(memoryTracker.getNote());
    if (currentFingerprint.equals(mcq.getFocusNoteFingerprint())) {
      memoryTrackerLifecycle.deleteStaleUnansweredRecallPrompts(memoryTracker, currentFingerprint);
    }
  }
}
