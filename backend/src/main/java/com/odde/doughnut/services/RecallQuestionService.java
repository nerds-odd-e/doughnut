package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecallQuestionService {
  // TODO: Read from DB in future
  private static final String TRUE_FALSE_PROMPT =
      """
      **True/False Question Guidelines**:
      - Create a clear statement that can be judged as True or False based on the note content.
      - The statement should be definitively True or False, not ambiguous or opinion-based.
      - Use markdown formatting if needed.
      - IMPORTANT: The choices MUST be exactly ["True", "False"] in this order.
      - Set correctChoiceIndex to 0 if the statement is True, 1 if False.
      - Set strictChoiceOrder to true (do not shuffle True/False choices).
      - Balance: Generate both true and false statements with roughly equal probability.
      - For false statements, make subtle but clear errors (not obviously wrong).
      - Test understanding of key concepts, not trivial details.
      """;

  private final PredefinedQuestionService predefinedQuestionService;
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
      PredefinedQuestionService predefinedQuestionService,
      AiQuestionGenerator aiQuestionGenerator) {
    this.recallPromptRepository = recallPromptRepository;
    this.entityPersister = entityPersister;
    this.answerService = answerService;
    this.memoryTrackerService = memoryTrackerService;
    this.predefinedQuestionService = predefinedQuestionService;
    this.aiQuestionGenerator = aiQuestionGenerator;
  }

  public RecallPrompt generateAQuestion(MemoryTracker memoryTracker) {
    // Spelling memory trackers should not have AI-generated questions
    if (Boolean.TRUE.equals(memoryTracker.getSpelling())) {
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
    // TODO: Read custom prompt from DB. For now, hardcode to True/False
    PredefinedQuestion question =
        predefinedQuestionService.generateAFeasibleQuestion(
            memoryTracker.getNote(), TRUE_FALSE_PROMPT);
    if (question == null) {
      return null;
    }
    return createARecallPromptFromQuestion(question, memoryTracker);
  }

  public RecallPrompt regenerateAQuestion(
      QuestionContestResult contestResult,
      Note note,
      MCQWithAnswer mcqWithAnswer,
      RecallPrompt existingRecallPrompt) {
    MCQWithAnswer MCQWithAnswer =
        aiQuestionGenerator.regenerateQuestion(contestResult, note, mcqWithAnswer);
    if (MCQWithAnswer == null) {
      return null;
    }
    PredefinedQuestion question = PredefinedQuestion.fromMCQWithAnswer(MCQWithAnswer, note);
    entityPersister.save(question);
    MemoryTracker memoryTracker = existingRecallPrompt.getMemoryTracker();
    return createARecallPromptFromQuestion(question, memoryTracker);
  }

  private RecallPrompt createARecallPromptFromQuestion(
      PredefinedQuestion question, MemoryTracker memoryTracker) {
    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setPredefinedQuestion(question);
    recallPrompt.setMemoryTracker(memoryTracker);
    recallPrompt.setQuestionType(QuestionType.MCQ);
    return entityPersister.save(recallPrompt);
  }

  public QuestionContestResult contest(RecallPrompt recallPrompt) {
    return predefinedQuestionService.contest(recallPrompt.getPredefinedQuestion());
  }

  public AnsweredQuestion answerQuestion(
      RecallPrompt recallPrompt, AnswerDTO answerDTO, User user, Timestamp currentUTCTimestamp) {
    Answer answer = answerService.createAnswerForQuestion(recallPrompt, answerDTO);
    boolean thresholdExceeded =
        memoryTrackerService.updateMemoryTrackerAfterAnsweringQuestion(
            user, currentUTCTimestamp, answer.getCorrect(), recallPrompt);
    AnsweredQuestion answeredQuestion = recallPrompt.getAnsweredQuestion();
    answeredQuestion.thresholdExceeded = thresholdExceeded;
    return answeredQuestion;
  }
}
