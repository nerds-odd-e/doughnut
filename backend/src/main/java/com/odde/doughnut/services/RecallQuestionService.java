package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
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
      PredefinedQuestion predefinedQuestion, QuestionContestResult contestResult) {
    Note note = predefinedQuestion.getNote();
    AiQuestionFactory aiQuestionFactory =
        new AiQuestionFactory(note, aiQuestionGenerator, predefinedQuestion, contestResult);
    PredefinedQuestion question = null;
    try {
      question = aiQuestionFactory.buildValidPredefinedQuestion();
    } catch (PredefinedQuestionNotPossibleException e) {
      return null;
    }
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
