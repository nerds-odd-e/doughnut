package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
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

  QuizQuestionAndAnswer selectQuizQuestionForANote(Note note) {
    return note.getQuizQuestionAndAnswers().stream().findFirst().orElse(null);
  }

  public QuizQuestionAndAnswer addQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    QuizQuestionAndAnswer quizQuestionAndAnswer =
        QuizQuestionAndAnswer.fromMCQWithAnswer(mcqWithAnswer, note);
    modelFactoryService.save(quizQuestionAndAnswer);
    return quizQuestionAndAnswer;
  }

  public MCQWithAnswer refineQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    return aiQuestionGenerator.getAiGeneratedRefineQuestion(note, mcqWithAnswer);
  }

  public QuizQuestionAndAnswer toggleApproval(QuizQuestionAndAnswer question) {
    question.setApproved(!question.isApproved());
    modelFactoryService.save(question);
    return question;
  }

  public MCQWithAnswer generateMcqWithAnswer(Note note) {
    return aiQuestionGenerator.getAiGeneratedQuestion(note);
  }

  public QuizQuestionAndAnswer generateQuestionForNote(Note note) {
    MCQWithAnswer MCQWithAnswer = generateMcqWithAnswer(note);
    if (MCQWithAnswer == null) {
      return null;
    }
    QuizQuestionAndAnswer quizQuestionAndAnswer =
        QuizQuestionAndAnswer.fromMCQWithAnswer(MCQWithAnswer, note);
    // make sure the id is the same as the quiz question id
    QuizQuestionAndAnswer saved = modelFactoryService.save(quizQuestionAndAnswer);
    saved.getQuizQuestion().setId(saved.getId());
    return saved;
  }
}
