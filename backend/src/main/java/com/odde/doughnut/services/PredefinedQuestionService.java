package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionGenerator;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;

public class PredefinedQuestionService {
  private final ModelFactoryService modelFactoryService;
  private final Randomizer randomizer;
  private final AiQuestionGenerator aiQuestionGenerator;

  public PredefinedQuestionService(
      OpenAiApi openAiApi, ModelFactoryService modelFactoryService, Randomizer randomizer) {
    this.modelFactoryService = modelFactoryService;
    this.randomizer = randomizer;
    this.aiQuestionGenerator =
        new AiQuestionGenerator(
            openAiApi, new GlobalSettingsService(modelFactoryService), randomizer);
  }

  public PredefinedQuestion addQuestion(Note note, PredefinedQuestion predefinedQuestion) {
    predefinedQuestion.setNote(note);

    Notebook parentNotebook = note.getNotebook();
    parentNotebook.setUpdated_at(new Timestamp(System.currentTimeMillis()));
    modelFactoryService.save(parentNotebook);
    modelFactoryService.save(predefinedQuestion);
    return predefinedQuestion;
  }

  public PredefinedQuestion refineAIQuestion(Note note, PredefinedQuestion predefinedQuestion) {
    MCQWithAnswer aiGeneratedRefineQuestion =
        aiQuestionGenerator.getAiGeneratedRefineQuestion(
            note, predefinedQuestion.getMcqWithAnswer());
    if (aiGeneratedRefineQuestion == null) {
      return null;
    }
    return PredefinedQuestion.fromMCQWithAnswer(aiGeneratedRefineQuestion, note);
  }

  public PredefinedQuestion toggleApproval(PredefinedQuestion question) {
    question.setApproved(!question.isApproved());
    modelFactoryService.save(question);
    return question;
  }

  public QuestionContestResult contest(PredefinedQuestion predefinedQuestion) {
    return aiQuestionGenerator.getQuestionContestResult(predefinedQuestion);
  }

  public PredefinedQuestion generateAQuestionOfRandomType(Note note, User user) {
    return generateAQuestionOfRandomType(note, user, null);
  }

  public PredefinedQuestion generateAQuestionOfRandomType(
      Note note, User user, QuestionContestResult contestResult) {
    PredefinedQuestion result =
        generateAQuestionOfRandomTypeWithoutSaving(note, user, contestResult);
    if (result == null) {
      return null;
    }
    modelFactoryService.save(result);
    return result;
  }

  public PredefinedQuestion generateAQuestionOfRandomTypeWithoutSaving(Note note, User user) {
    return generateAQuestionOfRandomTypeWithoutSaving(note, user, null);
  }

  public PredefinedQuestion generateAQuestionOfRandomTypeWithoutSaving(
      Note note, User user, QuestionContestResult contestResult) {
    PredefinedQuestionGenerator predefinedQuestionGenerator =
        new PredefinedQuestionGenerator(user, note, randomizer, modelFactoryService);
    return predefinedQuestionGenerator.generateAQuestionOfRandomType(
        aiQuestionGenerator, contestResult);
  }
}
