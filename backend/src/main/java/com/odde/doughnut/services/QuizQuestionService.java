package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.theokanning.openai.client.OpenAiApi;

public class QuizQuestionService {
  private final ModelFactoryService modelFactoryService;

  private final AiQuestionGenerator aiQuestionGenerator;

  public QuizQuestionService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
    this.aiQuestionGenerator =
        new AiQuestionGenerator(openAiApi, new GlobalSettingsService(modelFactoryService));
  }

  QuizQuestion selectQuizQuestionForANote(Note note) {
    return note.getQuizQuestions().stream().findFirst().orElse(null);
  }

  public QuizQuestion addQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    QuizQuestion quizQuestion = QuizQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
    modelFactoryService.save(quizQuestion);
    return quizQuestion;
  }

  public MCQWithAnswer refineQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    return aiQuestionGenerator.getAiGeneratedRefineQuestion(note, mcqWithAnswer);
  }

  public QuizQuestion toggleApproval(QuizQuestion question) {
    question.setApproved(!question.isApproved());
    modelFactoryService.save(question);
    return question;
  }

  public MCQWithAnswer generateMcqWithAnswer(Note note) {
    return aiQuestionGenerator.getAiGeneratedQuestion(note);
  }

  public QuizQuestion generateQuestionForNote(Note note) {
    MCQWithAnswer MCQWithAnswer = generateMcqWithAnswer(note);
    if (MCQWithAnswer == null) {
      return null;
    }
    QuizQuestion quizQuestion = QuizQuestion.fromMCQWithAnswer(MCQWithAnswer, note);
    return modelFactoryService.save(quizQuestion);
  }
}
