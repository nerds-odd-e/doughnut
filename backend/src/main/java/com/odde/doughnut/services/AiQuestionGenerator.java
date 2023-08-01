package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;

public class AiQuestionGenerator {
  private final Note note;
  private final OpenAiApiHandler openAiApiHandler;

  public AiQuestionGenerator(Note note, OpenAiApiHandler openAiApiHandler) {
    this.note = note;
    this.openAiApiHandler = openAiApiHandler;
  }

  public AIGeneratedQuestion getAiGeneratedQuestion() throws QuizQuestionNotPossibleException {
    return AIGeneratedQuestion.getValidQuestion(getAiGeneratedQuestionJson());
  }

  private JsonNode getAiGeneratedQuestionJson() throws QuizQuestionNotPossibleException {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder(note.getPath())
            .detailsOfNoteOfCurrentFocus(note)
            .userInstructionToGenerateQuestion()
            .maxTokens(1500)
            .build();

    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .orElseThrow(QuizQuestionNotPossibleException::new);
  }
}
