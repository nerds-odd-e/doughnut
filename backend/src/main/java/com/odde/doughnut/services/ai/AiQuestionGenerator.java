package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.ReviewQuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NotebookAssistantForNoteService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;

public record AiQuestionGenerator(
    OpenAiApi openAiApi, GlobalSettingsService globalSettingsService) {

  public MCQWithAnswer getAiGeneratedQuestion(Note note) {
    NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory =
        new NotebookAssistantForNoteServiceFactory(openAiApi, globalSettingsService);
    NotebookAssistantForNoteService service = notebookAssistantForNoteServiceFactory.create(note);
    try {
      return service.generateQuestion().orElse(null);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public MCQWithAnswer getAiGeneratedRefineQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    return forNote(note, globalSettingsService.globalSettingQuestionGeneration().getValue())
        .refineQuestion(mcqWithAnswer)
        .orElse(null);
  }

  public ReviewQuestionContestResult getReviewQuestionContestResult(
      PredefinedQuestion predefinedQuestion) {
    return forNote(
            predefinedQuestion.getNote(),
            globalSettingsService.globalSettingEvaluation().getValue())
        .evaluateQuestion(predefinedQuestion.getMcqWithAnswer())
        .map(e -> e.getReviewQuestionContestResult(predefinedQuestion.getCorrectAnswerIndex()))
        .orElse(null);
  }

  // Temporary method until we migrate all functionality
  private AiQuestionGeneratorForNote forNote(Note note, String modelName1) {
    OpenAIChatRequestBuilder chatAboutNoteRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName1, note);
    return new AiQuestionGeneratorForNote(
        new OpenAiApiHandler(openAiApi), chatAboutNoteRequestBuilder);
  }
}
