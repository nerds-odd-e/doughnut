package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      RecallPromptRepository recallPromptRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      ObjectMapper objectMapper,
      AnswerService answerService,
      GlobalSettingsService globalSettingsService,
      MemoryTrackerService memoryTrackerService) {
    this.recallPromptRepository = recallPromptRepository;
    this.entityPersister = entityPersister;
    this.answerService = answerService;
    this.memoryTrackerService = memoryTrackerService;
    aiQuestionGenerator =
        new AiQuestionGenerator(
            openAiApi, globalSettingsService, testabilitySettings.getRandomizer(), objectMapper);
    this.predefinedQuestionService =
        new PredefinedQuestionService(entityPersister, aiQuestionGenerator);
  }

  public RecallPrompt generateAQuestion(MemoryTracker memoryTracker) {
    // First check if there's an existing unanswered recall prompt for this note
    RecallPrompt existingPrompt = findExistingUnansweredRecallPrompt(memoryTracker.getNote());
    if (existingPrompt != null) {
      return existingPrompt;
    }

    return generateNewRecallPrompt(memoryTracker.getNote());
  }

  private RecallPrompt findExistingUnansweredRecallPrompt(Note note) {
    List<RecallPrompt> results = recallPromptRepository.findUnansweredByNote(note);
    return results.isEmpty() ? null : results.get(0);
  }

  private RecallPrompt generateNewRecallPrompt(Note note) {
    PredefinedQuestion question = predefinedQuestionService.generateAFeasibleQuestion(note);
    if (question == null) {
      return null;
    }
    return createARecallPromptFromQuestion(question);
  }

  public RecallPrompt regenerateAQuestion(
      QuestionContestResult contestResult, Note note, MCQWithAnswer mcqWithAnswer) {
    MCQWithAnswer MCQWithAnswer =
        aiQuestionGenerator.regenerateQuestion(contestResult, note, mcqWithAnswer);
    if (MCQWithAnswer == null) {
      return null;
    }
    PredefinedQuestion question = PredefinedQuestion.fromMCQWithAnswer(MCQWithAnswer, note);
    entityPersister.save(question);
    return createARecallPromptFromQuestion(question);
  }

  private RecallPrompt createARecallPromptFromQuestion(PredefinedQuestion question) {
    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setPredefinedQuestion(question);
    return entityPersister.save(recallPrompt);
  }

  public QuestionContestResult contest(RecallPrompt recallPrompt) {
    return predefinedQuestionService.contest(recallPrompt.getPredefinedQuestion());
  }

  public AnsweredQuestion answerQuestion(
      RecallPrompt recallPrompt, AnswerDTO answerDTO, User user, Timestamp currentUTCTimestamp) {
    Answer answer = answerService.createAnswerForQuestion(recallPrompt, answerDTO);
    memoryTrackerService.updateMemoryTrackerAfterAnsweringQuestion(
        user, currentUTCTimestamp, answer.getCorrect(), recallPrompt);
    return recallPrompt.getAnsweredQuestion();
  }
}
