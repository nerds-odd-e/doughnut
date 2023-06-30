package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import java.util.List;
import java.util.Optional;

public class OpenAiAPIChatCompletion extends OpenAiApiHandlerBase {

  private final OpenAiApi openAiApi;

  public OpenAiAPIChatCompletion(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public AIGeneratedQuestion getOpenAiGenerateQuestion(List<ChatMessage> chatMessages) {
    ChatCompletionRequest chatRequest = getChatRequestForGeneratingQuestion(chatMessages);
    return chatCompletion(chatRequest)
        .map(ChatCompletionChoice::getMessage)
        .map(ChatMessage::getFunctionCall)
        .map(ChatFunctionCall::getArguments)
        .map(
            arguments -> {
              try {
                return new ObjectMapper().treeToValue(arguments, AIGeneratedQuestion.class);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .orElse(null);
  }

  private ChatCompletionRequest getChatRequestForGeneratingQuestion(
      List<ChatMessage> chatMessages) {
    ChatFunction askSingleAnswerMultipleChoiceQuestion =
        ChatFunction.builder()
            .name("ask_single_answer_multiple_choice_question")
            .description("Ask a single-answer multiple-choice question to the user")
            .executor(AIGeneratedQuestion.class, null)
            .build();

    return OpenAIChatAboutNoteMessageBuilder.defaultChatCompletionRequestBuilder(chatMessages)
        .functions(List.of(askSingleAnswerMultipleChoiceQuestion))
        .maxTokens(1500)
        .build();
  }

  public Optional<ChatCompletionChoice> chatCompletion(ChatCompletionRequest request) {
    return withExceptionHandler(
        () ->
            openAiApi.createChatCompletion(request).blockingGet().getChoices().stream()
                .findFirst());
  }
}
