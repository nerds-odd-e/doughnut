package com.odde.doughnut.services.ai;

import com.odde.doughnut.controllers.json.QuizQuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;

public record AiQuestionGenerator(
    OpenAiApiHandler openAiApiHandler, GlobalSettingsService globalSettingsService) {
  private AiQuestionGeneratorForNote forNote(Note note, String modelName1) {
    OpenAIChatRequestBuilder chatAboutNoteRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName1, note);
    return new AiQuestionGeneratorForNote(openAiApiHandler, chatAboutNoteRequestBuilder);
  }

  public MCQWithAnswer getAiGeneratedQuestion(Note note) {
    return forNote(note, globalSettingsService.getGlobalSettingQuestionGeneration().getValue())
        .getAiGeneratedQuestion();
  }

  public QuizQuestionContestResult getQuizQuestionContestResult(
      QuizQuestionAIQuestion quizQuestionEntity) {
    return forNote(
            quizQuestionEntity.getNote(),
            globalSettingsService.getGlobalSettingEvaluation().getValue())
        .evaluateQuestion(quizQuestionEntity.getMcqWithAnswer())
        .map(e -> e.getQuizQuestionContestResult(quizQuestionEntity.getCorrectAnswerIndex()))
        .orElse(null);
  }
}
