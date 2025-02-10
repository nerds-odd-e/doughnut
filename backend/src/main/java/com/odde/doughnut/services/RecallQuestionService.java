package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;

public class RecallQuestionService {
  private final PredefinedQuestionService predefinedQuestionService;
  private final ModelFactoryService modelFactoryService;
  private final AiQuestionGenerator aiQuestionGenerator;

  public RecallQuestionService(
      OpenAiApi openAiApi, ModelFactoryService modelFactoryService, Randomizer randomizer) {
    this.modelFactoryService = modelFactoryService;
    aiQuestionGenerator =
        new AiQuestionGenerator(
            openAiApi, new GlobalSettingsService(modelFactoryService), randomizer);
    this.predefinedQuestionService =
        new PredefinedQuestionService(modelFactoryService, randomizer, aiQuestionGenerator);
  }

  public RecallPrompt generateAQuestion(MemoryTracker memoryTracker, User user) {
    PredefinedQuestion question = predefinedQuestionService.generateAQuestion(memoryTracker, user);
    if (question == null) {
      return null;
    }
    return createARecallPromptFromQuestion(question);
  }

  public RecallPrompt regenerateAQuestionOfRandomType(
      PredefinedQuestion predefinedQuestion, QuestionContestResult contestResult)
      throws JsonProcessingException {
    Note note = predefinedQuestion.getNote();
    MessageRequest additionalMessage =
        AiToolFactory.buildRegenerateQuestionMessage(predefinedQuestion, contestResult);
    MCQWithAnswer MCQWithAnswer =
        aiQuestionGenerator.getAiGeneratedQuestion(note, additionalMessage);
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
      RecallPrompt recallPrompt, AnswerDTO answerDTO, User user, Timestamp currentUTCTimestamp) {
    Answer answer = modelFactoryService.createAnswerForQuestion(recallPrompt, answerDTO);
    MemoryTrackerService memoryTrackerService = new MemoryTrackerService(modelFactoryService);
    memoryTrackerService.updateMemoryTrackerAfterAnsweringQuestion(
        user, currentUTCTimestamp, answer.getCorrect(), recallPrompt);
    return recallPrompt.getAnsweredQuestion();
  }
}
