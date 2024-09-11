package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuestionAndAnswer;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.theokanning.openai.client.OpenAiApi;
import jakarta.validation.Valid;
import java.sql.Timestamp;

public class QuizQuestionService {
  private final ModelFactoryService modelFactoryService;

  private final AiQuestionGenerator aiQuestionGenerator;

  public QuizQuestionService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
    this.aiQuestionGenerator =
        new AiQuestionGenerator(openAiApi, new GlobalSettingsService(modelFactoryService));
  }

  public QuestionAndAnswer addQuestion(Note note, @Valid QuestionAndAnswer questionAndAnswer) {
    questionAndAnswer.setNote(note);

    Notebook parentNotebook = note.getNotebook();
    parentNotebook.setUpdated_at(new Timestamp(System.currentTimeMillis()));
    modelFactoryService.save(parentNotebook);
    questionAndAnswer.getQuizQuestion().setQuestionAndAnswer(questionAndAnswer);
    modelFactoryService.save(questionAndAnswer);
    return questionAndAnswer;
  }

  public QuestionAndAnswer refineQuestion(Note note, QuestionAndAnswer questionAndAnswer) {
    MCQWithAnswer aiGeneratedRefineQuestion =
        aiQuestionGenerator.getAiGeneratedRefineQuestion(
            note, questionAndAnswer.getMcqWithAnswer());
    if (aiGeneratedRefineQuestion == null) {
      return null;
    }
    return QuestionAndAnswer.fromMCQWithAnswer(aiGeneratedRefineQuestion, note);
  }

  public QuestionAndAnswer toggleApproval(QuestionAndAnswer question) {
    question.setApproved(!question.isApproved());
    modelFactoryService.save(question);
    return question;
  }

  public QuestionAndAnswer generateMcqWithAnswer(Note note) {
    MCQWithAnswer MCQWithAnswer = aiQuestionGenerator.getAiGeneratedQuestion(note);
    if (MCQWithAnswer == null) {
      return null;
    }
    return QuestionAndAnswer.fromMCQWithAnswer(MCQWithAnswer, note);
  }

  public QuestionAndAnswer generateQuestionForNote(Note note) {
    QuestionAndAnswer question = generateMcqWithAnswer(note);
    if (question == null) {
      return null;
    }
    return modelFactoryService.save(question);
  }
}
