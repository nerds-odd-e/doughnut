package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PredefinedQuestionService {
  private final EntityPersister entityPersister;
  private final AiQuestionGenerator aiQuestionGenerator;

  @Autowired
  public PredefinedQuestionService(
      EntityPersister entityPersister,
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      GlobalSettingsService globalSettingsService,
      TestabilitySettings testabilitySettings,
      ObjectMapper objectMapper) {
    this.entityPersister = entityPersister;
    this.aiQuestionGenerator =
        new AiQuestionGenerator(
            openAiApi, globalSettingsService, testabilitySettings.getRandomizer(), objectMapper);
  }

  // Test-only constructor for injecting mocked AiQuestionGenerator
  public PredefinedQuestionService(
      EntityPersister entityPersister, AiQuestionGenerator aiQuestionGenerator) {
    this.entityPersister = entityPersister;
    this.aiQuestionGenerator = aiQuestionGenerator;
  }

  public PredefinedQuestion addQuestion(Note note, PredefinedQuestion predefinedQuestion) {
    predefinedQuestion.setNote(note);

    Notebook parentNotebook = note.getNotebook();
    parentNotebook.setUpdated_at(new Timestamp(System.currentTimeMillis()));
    entityPersister.save(parentNotebook);
    entityPersister.save(predefinedQuestion);
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
    entityPersister.save(question);
    return question;
  }

  public QuestionContestResult contest(PredefinedQuestion predefinedQuestion) {
    MCQWithAnswer mcqWithAnswer = predefinedQuestion.getMcqWithAnswer();
    QuestionEvaluation questionContestResult =
        aiQuestionGenerator.getQuestionContestResult(predefinedQuestion.getNote(), mcqWithAnswer);
    if (questionContestResult == null) {
      return null;
    }
    QuestionContestResult result = questionContestResult.getQuestionContestResult(mcqWithAnswer);
    if (!result.rejected) {
      predefinedQuestion.setContested(true);
      entityPersister.merge(predefinedQuestion);
    }
    return result;
  }

  public PredefinedQuestion generateAFeasibleQuestion(Note note) {
    MCQWithAnswer mcqWithAnswer = aiQuestionGenerator.getAiGeneratedQuestion(note, null);
    if (mcqWithAnswer == null) {
      return null;
    }

    PredefinedQuestion result = PredefinedQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
    entityPersister.save(result);

    // Auto-evaluate the generated question
    QuestionContestResult contestResult = contest(result);

    if (contestResult == null || contestResult.rejected) {
      return result;
    }

    // Try to regenerate with the contest feedback
    MCQWithAnswer regeneratedQuestion =
        aiQuestionGenerator.regenerateQuestion(contestResult, note, mcqWithAnswer);
    if (regeneratedQuestion != null) {
      // Create and save the regenerated question
      PredefinedQuestion regenerated =
          PredefinedQuestion.fromMCQWithAnswer(regeneratedQuestion, note);
      return entityPersister.save(regenerated);
    }
    return entityPersister.save(result);
  }
}
