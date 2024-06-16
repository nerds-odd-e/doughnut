package com.odde.doughnut.services.ai;

import com.odde.doughnut.controllers.dto.QuizQuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
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
    return forNote(note, globalSettingsService.getGlobalSettingQuestionGeneration().getValue())
        .getAiGeneratedQuestion();
  }

  public MCQWithAnswer getAiGeneratedRefineQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    return forNote(note, globalSettingsService.getGlobalSettingQuestionGeneration().getValue())
        .refineQuestion(mcqWithAnswer)
        .orElse(null);
  }

  public QuizQuestionContestResult getQuizQuestionContestResult(
      QuizQuestionAndAnswer quizQuestionAndAnswer) {
    return forNote(
            quizQuestionAndAnswer.getNote(),
            globalSettingsService.getGlobalSettingEvaluation().getValue())
        .evaluateQuestion(quizQuestionAndAnswer.getMcqWithAnswer())
        .map(e -> e.getQuizQuestionContestResult(quizQuestionAndAnswer.getCorrectAnswerIndex()))
        .orElse(null);
  }
}
