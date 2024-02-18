package com.odde.doughnut.services.ai;

import com.odde.doughnut.controllers.json.QuizQuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;

public record AiQuestionGenerator(OpenAiApiHandler openAiApiHandler, String modelName) {
  public AiQuestionGeneratorForNote forNote(Note note) {
    OpenAIChatRequestBuilder chatAboutNoteRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName, note);
    return new AiQuestionGeneratorForNote(openAiApiHandler, chatAboutNoteRequestBuilder);
  }

  public MCQWithAnswer getAiGeneratedQuestion(Note note) {
    return forNote(note)
      .getAiGeneratedQuestion();
  }


  public QuizQuestionContestResult getQuizQuestionContestResult(QuizQuestionAIQuestion quizQuestionEntity) {
    return forNote(quizQuestionEntity.getNote())
      .evaluateQuestion(quizQuestionEntity.getMcqWithAnswer())
      .map(e -> e.getQuizQuestionContestResult(quizQuestionEntity.getCorrectAnswerIndex()))
      .orElse(null);
  }
}
