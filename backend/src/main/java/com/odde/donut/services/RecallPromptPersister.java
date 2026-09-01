package com.odde.donut.services;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.factoryServices.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Re-checks for an unanswered prompt inside the write transaction to avoid a concurrent duplicate.
@Service
public class RecallPromptPersister {
  private final RecallPromptRepository recallPromptRepository;
  private final EntityPersister entityPersister;

  @Autowired
  public RecallPromptPersister(
      RecallPromptRepository recallPromptRepository, EntityPersister entityPersister) {
    this.recallPromptRepository = recallPromptRepository;
    this.entityPersister = entityPersister;
  }

  @Transactional
  public RecallPrompt persistRecallPromptForMcq(Mcq mcq, MemoryTracker memoryTracker) {
    RecallPrompt existingPrompt =
        recallPromptRepository.findUnansweredByMemoryTracker(memoryTracker.getId()).orElse(null);
    if (existingPrompt != null) {
      return existingPrompt;
    }
    return entityPersister.save(RecallPrompt.forMcq(mcq, memoryTracker));
  }
}
