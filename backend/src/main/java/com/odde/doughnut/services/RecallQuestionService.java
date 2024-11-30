package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.ReviewQuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;

public class RecallQuestionService {
  private final PredefinedQuestionService predefinedQuestionService;
  private final ModelFactoryService modelFactoryService;

  public RecallQuestionService(
      OpenAiApi openAiApi, ModelFactoryService modelFactoryService, Randomizer randomizer) {
    this.modelFactoryService = modelFactoryService;
    this.predefinedQuestionService =
        new PredefinedQuestionService(openAiApi, modelFactoryService, randomizer);
  }

  public RecallPrompt generateAQuestionOfRandomType(Note note, User user) {
    PredefinedQuestion question =
        predefinedQuestionService.generateAQuestionOfRandomType(note, user);
    if (question == null) {
      return null;
    }
    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setPredefinedQuestion(question);
    return modelFactoryService.save(recallPrompt);
  }

  public ReviewQuestionContestResult contest(RecallPrompt recallPrompt) {
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
