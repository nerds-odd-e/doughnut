package com.odde.donut.services;

import com.odde.donut.controllers.dto.AnswerDTO;
import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.*;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.ai.AiQuestionGenerator;
import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecallQuestionService {
  private final McqService mcqService;
  private final RecallPromptRepository recallPromptRepository;
  private final EntityPersister entityPersister;
  private final AiQuestionGenerator aiQuestionGenerator;
  private final AnswerService answerService;
  private final MemoryTrackerService memoryTrackerService;
  private final RecallPromptPersister recallPromptPersister;

  @Autowired
  public RecallQuestionService(
      RecallPromptRepository recallPromptRepository,
      EntityPersister entityPersister,
      AnswerService answerService,
      MemoryTrackerService memoryTrackerService,
      McqService mcqService,
      AiQuestionGenerator aiQuestionGenerator,
      RecallPromptPersister recallPromptPersister) {
    this.recallPromptRepository = recallPromptRepository;
    this.entityPersister = entityPersister;
    this.answerService = answerService;
    this.memoryTrackerService = memoryTrackerService;
    this.mcqService = mcqService;
    this.aiQuestionGenerator = aiQuestionGenerator;
    this.recallPromptPersister = recallPromptPersister;
  }

  public RecallPrompt generateAQuestion(MemoryTracker memoryTracker) {
    // Spelling memory trackers should not have AI-generated questions
    if (memoryTracker.isSpelling()) {
      return null;
    }

    // First check if there's an existing unanswered recall prompt for this note and memory tracker
    RecallPrompt existingPrompt = findExistingUnansweredRecallPrompt(memoryTracker);
    if (existingPrompt != null) {
      return existingPrompt;
    }

    return generateNewRecallPrompt(memoryTracker);
  }

  private RecallPrompt findExistingUnansweredRecallPrompt(MemoryTracker memoryTracker) {
    return recallPromptRepository.findUnansweredByMemoryTracker(memoryTracker.getId()).orElse(null);
  }

  private RecallPrompt generateNewRecallPrompt(MemoryTracker memoryTracker) {
    Note note = memoryTracker.getNote();
    Mcq mcq = mcqService.generateAFeasibleQuestion(note, memoryTracker.getPropertyKey());
    if (mcq == null) {
      return null;
    }
    return recallPromptPersister.persistRecallPromptForMcq(mcq, memoryTracker);
  }

  public RecallPrompt regenerateAQuestion(
      QuestionContestResult contestResult,
      Note note,
      Mcq existingMcq,
      RecallPrompt existingRecallPrompt) {
    long contextSeed = ThreadLocalRandom.current().nextLong();
    Long contextSeedBoxed = Long.valueOf(contextSeed);
    MemoryTracker memoryTracker = existingRecallPrompt.requireMemoryTracker();
    Mcq mcq =
        aiQuestionGenerator.regenerateQuestion(
            contestResult, note, existingMcq, contextSeedBoxed, memoryTracker.getPropertyKey());
    if (mcq == null) {
      return null;
    }
    entityPersister.save(mcq);
    return createARecallPromptFromMcq(mcq, memoryTracker);
  }

  private RecallPrompt createARecallPromptFromMcq(Mcq mcq, MemoryTracker memoryTracker) {
    return entityPersister.save(RecallPrompt.forMcq(mcq, memoryTracker));
  }

  public QuestionContestResult contest(RecallPrompt recallPrompt) {
    return mcqService.contest(recallPrompt.getMcq());
  }

  public RecallPrompt answer(
      RecallPrompt recallPrompt, AnswerDTO answerDTO, Timestamp currentUTCTimestamp) {
    Answer answer =
        answerService.createAnswerForQuestion(recallPrompt, answerDTO, currentUTCTimestamp);
    memoryTrackerService.updateMemoryTrackerAfterAnsweringQuestion(
        currentUTCTimestamp, answer.getCorrect(), recallPrompt);
    return recallPrompt;
  }
}
