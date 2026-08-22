package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
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

  @Autowired
  public RecallQuestionService(
      RecallPromptRepository recallPromptRepository,
      EntityPersister entityPersister,
      AnswerService answerService,
      MemoryTrackerService memoryTrackerService,
      McqService mcqService,
      AiQuestionGenerator aiQuestionGenerator) {
    this.recallPromptRepository = recallPromptRepository;
    this.entityPersister = entityPersister;
    this.answerService = answerService;
    this.memoryTrackerService = memoryTrackerService;
    this.mcqService = mcqService;
    this.aiQuestionGenerator = aiQuestionGenerator;
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
    return createARecallPromptFromMcq(mcq, memoryTracker);
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
    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setMcq(mcq);
    recallPrompt.setMemoryTracker(memoryTracker);
    recallPrompt.setQuestionType(QuestionType.MCQ);
    return entityPersister.save(recallPrompt);
  }

  public QuestionContestResult contest(RecallPrompt recallPrompt) {
    return mcqService.contest(recallPrompt.getMcq());
  }

  public RecallPrompt answer(
      RecallPrompt recallPrompt, AnswerDTO answerDTO, Timestamp currentUTCTimestamp) {
    Answer answer = answerService.createAnswerForQuestion(recallPrompt, answerDTO);
    memoryTrackerService.updateMemoryTrackerAfterAnsweringQuestion(
        currentUTCTimestamp, answer.getCorrect(), recallPrompt);
    return recallPrompt;
  }
}
