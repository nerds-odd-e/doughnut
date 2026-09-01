package com.odde.donut.services;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.factoryServices.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists a recall prompt for an already AI-generated {@link Mcq} in one short write transaction,
 * invoked only after the OpenAI call has returned. Re-checks for an unanswered recall prompt inside
 * that same transaction so a concurrent request that already created one is reused instead of
 * inserting a duplicate.
 */
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
