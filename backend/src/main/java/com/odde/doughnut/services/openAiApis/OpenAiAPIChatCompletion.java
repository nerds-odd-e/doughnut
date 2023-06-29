package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import java.util.List;
import java.util.Optional;

public class OpenAiAPIChatCompletion extends OpenAiApiHandlerBase {

  private final OpenAiApi openAiApi;

  public OpenAiAPIChatCompletion(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public AiSuggestion getOpenAiCompletion(List<ChatMessage> chatMessages) {
    return chatCompletion(defaultChatCompletionRequestBuilder(chatMessages).maxTokens(100).build())
        .map(AiSuggestion::from)
        .orElse(null);
  }

  public String getOpenAiGenerateQuestion(List<ChatMessage> chatMessages) {
    ChatCompletionRequest chatRequest = getChatRequestForGeneratingQuestion(chatMessages);
    return chatCompletion(chatRequest)
        .map(ChatCompletionChoice::getMessage)
        .map(ChatMessage::getFunctionCall)
        .map(ChatFunctionCall::getArguments)
        .map(JsonNode::toString)
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

    return defaultChatCompletionRequestBuilder(chatMessages)
        .functions(List.of(askSingleAnswerMultipleChoiceQuestion))
        .functionCall(
            new ChatCompletionRequest.ChatCompletionRequestFunctionCall(
                "ask_single_answer_multiple_choice_question"))
        .maxTokens(1100)
        .build();
  }

  private Optional<ChatCompletionChoice> chatCompletion(ChatCompletionRequest request) {
    return withExceptionHandler(
        () ->
            openAiApi.createChatCompletion(request).blockingGet().getChoices().stream()
                .findFirst());
  }

  private static ChatCompletionRequest.ChatCompletionRequestBuilder
      defaultChatCompletionRequestBuilder(List<ChatMessage> messages) {
    return ChatCompletionRequest.builder()
        .model("gpt-3.5-turbo")
        .messages(messages)
        //
        // an effort has been made to make the api call more responsive by using stream(true)
        // however, due to the library limitation, we cannot do it yet.
        // find more details here:
        //    https://github.com/TheoKanning/openai-java/issues/83
        .stream(false)
        .n(1);
  }
}
