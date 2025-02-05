package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
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
    return generateAQuestionOfRandomType(note, user, null);
  }

  public RecallPrompt generateAQuestionOfRandomType(
      Note note, User user, QuestionContestResult contestResult) {
    PredefinedQuestion question =
        predefinedQuestionService.generateAQuestionOfRandomType(note, user, contestResult);
    if (question == null) {
      return null;
    }
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
