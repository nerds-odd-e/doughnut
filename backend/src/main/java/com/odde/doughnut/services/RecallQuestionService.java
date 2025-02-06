package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
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

  public RecallPrompt generateAQuestionOfRandomType(Note note, User user) {
    PredefinedQuestion question =
        predefinedQuestionService.generateAQuestionOfRandomType(
            note, user, new AiQuestionFactory(note, aiQuestionGenerator));
    if (question == null) {
      return null;
    }
    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setPredefinedQuestion(question);
    return modelFactoryService.save(recallPrompt);
  }

  public RecallPrompt regenerateAQuestionOfRandomType(
      PredefinedQuestion predefinedQuestion, QuestionContestResult contestResult)
      throws JsonProcessingException {
    Note note = predefinedQuestion.getNote();
    MessageRequest additionalMessage =
        MessageRequest.builder()
            .role("user")
            .content(
                """
                  Previously generated non-feasible question:
                  %s

                  Non-feasible reason:
                  %s

                  Please regenerate or refine the question based on the above feedback."""
                    .formatted(
                        new ObjectMapper()
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(predefinedQuestion.getMcqWithAnswer()),
                        contestResult.reason))
            .build();
    MCQWithAnswer MCQWithAnswer =
        aiQuestionGenerator.getAiGeneratedQuestion(note, additionalMessage);
    if (MCQWithAnswer == null) {
      return null;
    }
    PredefinedQuestion question = PredefinedQuestion.fromMCQWithAnswer(MCQWithAnswer, note);
    modelFactoryService.save(question);
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
    modelFactoryService.updateMemoryTrackerAfterAnsweringQuestion(
        user, currentUTCTimestamp, answer.getCorrect(), recallPrompt);
    return recallPrompt.getAnsweredQuestion();
  }
}
