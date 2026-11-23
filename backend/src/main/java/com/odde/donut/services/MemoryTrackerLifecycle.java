package com.odde.donut.services;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.ConversationRepository;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.factoryServices.EntityPersister;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemoryTrackerLifecycle {
  private final EntityPersister entityPersister;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final McqRepository mcqRepository;
  private final ConversationRepository conversationRepository;

  public MemoryTrackerLifecycle(
      EntityPersister entityPersister,
      MemoryTrackerRepository memoryTrackerRepository,
      RecallPromptRepository recallPromptRepository,
      McqRepository mcqRepository,
      ConversationRepository conversationRepository) {
    this.entityPersister = entityPersister;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.mcqRepository = mcqRepository;
    this.conversationRepository = conversationRepository;
  }

  @Transactional
  public void delete(MemoryTracker memoryTracker) {
    deleteTrackers(List.of(memoryTracker));
  }

  @Transactional
  public void deleteForNote(Note note) {
    deleteTrackers(memoryTrackerRepository.findByNote_IdIn(List.of(note.getId())));
  }

  private void deleteTrackers(List<MemoryTracker> memoryTrackers) {
    List<Integer> questionIds =
        recallPromptRepository.findQuestionIdsByMemoryTrackerIds(
            memoryTrackers.stream().map(MemoryTracker::getId).toList());
    memoryTrackers.forEach(entityPersister::remove);
    mcqRepository.deleteUnreferencedByIds(questionIds);
  }

  @Transactional
  public void deleteUnansweredRecallPrompts(MemoryTracker memoryTracker) {
    deleteRecallPrompts(
        recallPromptRepository.findAllUnansweredByMemoryTrackerId(memoryTracker.getId()));
  }

  @Transactional
  public void deleteStaleUnansweredRecallPrompts(
      MemoryTracker memoryTracker, String currentFocusFingerprint) {
    deleteRecallPrompts(
        recallPromptRepository.findAllUnansweredMcqsForOtherFocus(
            memoryTracker.getId(), currentFocusFingerprint));
  }

  private void deleteRecallPrompts(List<RecallPrompt> prompts) {
    if (prompts.isEmpty()) {
      return;
    }
    List<Integer> questionIds =
        prompts.stream()
            .map(RecallPrompt::getMcq)
            .filter(Objects::nonNull)
            .map(Mcq::getId)
            .toList();
    conversationRepository
        .findBySubjectRecallPromptIn(prompts)
        .forEach(conversation -> {
          conversation.getSubject().setRecallPrompt(null);
          conversationRepository.save(conversation);
        });
    prompts.forEach(entityPersister::remove);
    mcqRepository.deleteUnreferencedByIds(questionIds);
  }
}
