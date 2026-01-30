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
    Note note = memoryTracker.getNote();
    String customPrompt = getCustomPromptForNote(note);
    PredefinedQuestion question =
        predefinedQuestionService.generateAFeasibleQuestion(note, customPrompt);
    if (question == null) {
      return null;
    }
    return createARecallPromptFromQuestion(question, memoryTracker);
  }

  private String getCustomPromptForNote(Note note) {
    // Check the note itself first
    NoteAiAssistant aiAssistant = note.getNoteAiAssistant();
    if (aiAssistant != null && hasValidPrompt(aiAssistant)) {
      return aiAssistant.getAdditionalInstructionsToAi();
    }

    // Check parent notes for applyToChildren
    Note parent = note.getParent();
    while (parent != null) {
      NoteAiAssistant parentAiAssistant = parent.getNoteAiAssistant();
      if (parentAiAssistant != null
          && Boolean.TRUE.equals(parentAiAssistant.getApplyToChildren())
          && hasValidPrompt(parentAiAssistant)) {
        return parentAiAssistant.getAdditionalInstructionsToAi();
      }
      parent = parent.getParent();
    }

    // Fallback to default MCQ prompt (null means use default)
    return null;
  }

  private boolean hasValidPrompt(NoteAiAssistant aiAssistant) {
    String prompt = aiAssistant.getAdditionalInstructionsToAi();
    return prompt != null && !prompt.isBlank();
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
