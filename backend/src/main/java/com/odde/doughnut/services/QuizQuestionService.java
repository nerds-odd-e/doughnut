package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestionDTO;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.theokanning.openai.client.OpenAiApi;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class QuizQuestionService {
  private final ModelFactoryService modelFactoryService;

  private final AiQuestionGenerator aiQuestionGenerator;

  public QuizQuestionService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
    this.aiQuestionGenerator =
        new AiQuestionGenerator(openAiApi, new GlobalSettingsService(modelFactoryService));
  }

  public QuizQuestion generateAIQuestion(Note note) {
    AiQuestionFactory aiQuestionFactory = new AiQuestionFactory(note, aiQuestionGenerator);
    try {
      QuizQuestion quizQuestion = aiQuestionFactory.buildValidQuizQuestion(null);
      modelFactoryService.save(quizQuestion);
      return quizQuestion;
    } catch (QuizQuestionNotPossibleException e) {
      throw (new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated"));
    }
  }

  public QuizQuestionDTO generateAIQuestionWithoutSave(Note note) {
    AiQuestionFactory aiQuestionFactory = new AiQuestionFactory(note, aiQuestionGenerator);
    try {
      QuizQuestion quizQuestion = aiQuestionFactory.buildValidQuizQuestion(null);
      return QuizQuestionDTO.builder()
          .multipleChoicesQuestion(quizQuestion.getMultipleChoicesQuestion())
          .correctAnswerIndex(quizQuestion.getCorrectAnswerIndex())
          .createdAt(quizQuestion.getCreatedAt())
          .build();
    } catch (QuizQuestionNotPossibleException e) {
      throw (new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated"));
    }
  }

  QuizQuestion selectQuizQuestionForANote(Note note) {
    return note.getQuizQuestions().stream().findFirst().orElse(null);
  }

  public QuizQuestion addQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    QuizQuestion quizQuestion = QuizQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
    modelFactoryService.save(quizQuestion);
    return quizQuestion;
  }

  public QuizQuestion refineQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    AiQuestionFactory aiQuestionFactory = new AiQuestionFactory(note, aiQuestionGenerator);
    try {
      return aiQuestionFactory.refineQuestion(mcqWithAnswer);
    } catch (QuizQuestionNotPossibleException e) {
      throw (new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated"));
    }
  }

  public QuizQuestion approveQuestion(QuizQuestion question) {
    question.setApproved(!question.isApproved());

    modelFactoryService.save(question);
    return question;
  }
}
