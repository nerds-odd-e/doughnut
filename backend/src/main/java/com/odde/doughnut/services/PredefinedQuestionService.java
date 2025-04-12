package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import java.sql.Timestamp;

public class PredefinedQuestionService {
  private final ModelFactoryService modelFactoryService;
  private final AiQuestionGenerator aiQuestionGenerator;

  public PredefinedQuestionService(
      ModelFactoryService modelFactoryService, AiQuestionGenerator aiQuestionGenerator) {
    this.modelFactoryService = modelFactoryService;
    this.aiQuestionGenerator = aiQuestionGenerator;
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
    QuestionContestResult result =
        aiQuestionGenerator.getQuestionContestResult(
            predefinedQuestion.getNote(), predefinedQuestion.getMcqWithAnswer());
    if (result != null && !result.rejected) {
      predefinedQuestion.setContested(true);
      modelFactoryService.merge(predefinedQuestion);
    }
    return result;
  }

  public PredefinedQuestion generateAFeasibleQuestion(Note note) {
    MCQWithAnswer mcqWithAnswer = aiQuestionGenerator.getAiGeneratedQuestion(note, null);
    if (mcqWithAnswer == null) {
      return null;
    }

    PredefinedQuestion result = PredefinedQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
    modelFactoryService.save(result);

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
      return modelFactoryService.save(regenerated);
    }
    return modelFactoryService.save(result);
  }
}
