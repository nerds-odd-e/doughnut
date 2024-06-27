package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.theokanning.openai.client.OpenAiApi;
import jakarta.validation.Valid;

public class QuizQuestionService {
  private final ModelFactoryService modelFactoryService;

  private final AiQuestionGenerator aiQuestionGenerator;

  public QuizQuestionService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
    this.aiQuestionGenerator =
        new AiQuestionGenerator(openAiApi, new GlobalSettingsService(modelFactoryService));
  }

  QuizQuestionAndAnswer selectQuizQuestionForANote(Note note) {
    return note.getQuizQuestionAndAnswers().stream().findFirst().orElse(null);
  }

  public QuizQuestionAndAnswer addQuestion(
      Note note, @Valid QuizQuestionAndAnswer questionAndAnswer) {
    questionAndAnswer.setNote(note);
    modelFactoryService.save(questionAndAnswer);
    return questionAndAnswer;
  }

  public void deleteQuestion(QuizQuestionAndAnswer quizQuestion) {
    modelFactoryService.remove(quizQuestion);
  }

  public QuizQuestionAndAnswer refineQuestion(Note note, QuizQuestionAndAnswer questionAndAnswer) {
    MCQWithAnswer aiGeneratedRefineQuestion =
        aiQuestionGenerator.getAiGeneratedRefineQuestion(
            note, questionAndAnswer.getMcqWithAnswer());
    if (aiGeneratedRefineQuestion == null) {
      return null;
    }
    return QuizQuestionAndAnswer.fromMCQWithAnswer(aiGeneratedRefineQuestion, note);
  }

  public QuizQuestionAndAnswer toggleApproval(QuizQuestionAndAnswer question) {
    question.setApproved(!question.isApproved());
    modelFactoryService.save(question);
    return question;
  }

  public QuizQuestionAndAnswer generateMcqWithAnswer(Note note) {
    MCQWithAnswer MCQWithAnswer = aiQuestionGenerator.getAiGeneratedQuestion(note);
    if (MCQWithAnswer == null) {
      return null;
    }
    return QuizQuestionAndAnswer.fromMCQWithAnswer(MCQWithAnswer, note);
  }

  public QuizQuestionAndAnswer generateQuestionForNote(Note note) {
    QuizQuestionAndAnswer question = generateMcqWithAnswer(note);
    if (question == null) {
      return null;
    }
    return modelFactoryService.save(question);
  }
}
