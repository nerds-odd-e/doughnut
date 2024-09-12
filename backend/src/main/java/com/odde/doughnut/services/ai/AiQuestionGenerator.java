package com.odde.doughnut.services.ai;

import com.odde.doughnut.controllers.dto.ReviewQuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;

public record AiQuestionGenerator(
    OpenAiApi openAiApi, GlobalSettingsService globalSettingsService) {
  private AiQuestionGeneratorForNote forNote(Note note, String modelName1) {
    OpenAIChatRequestBuilder chatAboutNoteRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName1, note);
    return new AiQuestionGeneratorForNote(
        new OpenAiApiHandler(openAiApi), chatAboutNoteRequestBuilder);
  }

  public MCQWithAnswer getAiGeneratedQuestion(Note note) {
    return forNote(note, globalSettingsService.globalSettingQuestionGeneration().getValue())
        .getAiGeneratedQuestion();
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
}
