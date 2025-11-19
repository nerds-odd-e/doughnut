package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.utils.Randomizer;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;
import java.util.List;

public class RecallQuestionService {
  private final PredefinedQuestionService predefinedQuestionService;
  private final ModelFactoryService modelFactoryService;
  private final AiQuestionGenerator aiQuestionGenerator;

  public RecallQuestionService(
      OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      Randomizer randomizer,
      ObjectMapper objectMapper) {
    this.modelFactoryService = modelFactoryService;
    aiQuestionGenerator =
        new AiQuestionGenerator(
            openAiApi, new GlobalSettingsService(modelFactoryService), randomizer, objectMapper);
    this.predefinedQuestionService =
        new PredefinedQuestionService(modelFactoryService, aiQuestionGenerator);
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
    List<RecallPrompt> results =
        modelFactoryService.recallPromptRepository.findUnansweredByNote(note);
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
    modelFactoryService.save(question);
    return createARecallPromptFromQuestion(question);
  }

  private RecallPrompt createARecallPromptFromQuestion(PredefinedQuestion question) {
    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setPredefinedQuestion(question);
    return modelFactoryService.save(recallPrompt);
  }

  public QuestionContestResult contest(RecallPrompt recallPrompt) {
    return predefinedQuestionService.contest(recallPrompt.getPredefinedQuestion());
  }

  public AnsweredQuestion answerQuestion(
      RecallPrompt recallPrompt,
      AnswerDTO answerDTO,
      User user,
      Timestamp currentUTCTimestamp,
      UserService userService) {
    Answer answer = modelFactoryService.createAnswerForQuestion(recallPrompt, answerDTO);
    MemoryTrackerService memoryTrackerService = new MemoryTrackerService(modelFactoryService);
    memoryTrackerService.updateMemoryTrackerAfterAnsweringQuestion(
        user, currentUTCTimestamp, answer.getCorrect(), recallPrompt, userService);
    return recallPrompt.getAnsweredQuestion();
  }
}
