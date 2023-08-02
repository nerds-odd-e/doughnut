package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class AiQuestionGenerator {
  private final Note note;
  private final OpenAiApiHandler openAiApiHandler;

  public AiQuestionGenerator(Note note, OpenAiApiHandler openAiApiHandler) {
    this.note = note;
    this.openAiApiHandler = openAiApiHandler;
  }

  public AIGeneratedQuestion getAiGeneratedQuestion() throws QuizQuestionNotPossibleException {
    boolean useGPT4 = shortContent();
    AIGeneratedQuestion question =
        AIGeneratedQuestion.getValidQuestion(getAiGeneratedQuestionJson(useGPT4));
    if (!useGPT4) {
      ChatCompletionRequest chatRequest =
          new OpenAIChatAboutNoteRequestBuilder(note.getPath())
              .detailsOfNoteOfCurrentFocus(note)
              .validateQuestionAgain(question)
              .maxTokens(1500)
              .build();

      Optional<JsonNode> makeSense = openAiApiHandler.getFunctionCallArguments(chatRequest);
      makeSense.ifPresent(jsonNode -> question.stem += jsonNode);
    }
    return question;
  }

  private JsonNode getAiGeneratedQuestionJson(boolean shortContent)
      throws QuizQuestionNotPossibleException {
    OpenAIChatAboutNoteRequestBuilder openAIChatAboutNoteRequestBuilder =
        new OpenAIChatAboutNoteRequestBuilder(note.getPath())
            .detailsOfNoteOfCurrentFocus(note)
            .userInstructionToGenerateQuestion()
            .maxTokens(1500);
    if (shortContent) {
      openAIChatAboutNoteRequestBuilder.useGPT4();
    }
    ChatCompletionRequest chatRequest = openAIChatAboutNoteRequestBuilder.build();

    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .orElseThrow(QuizQuestionNotPossibleException::new);
  }

  private boolean shortContent() {
    return note.getTitle().getBytes(StandardCharsets.UTF_8).length
            + note.getDescription().getBytes(StandardCharsets.UTF_8).length
        < 300;
  }
}
