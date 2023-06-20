package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
import java.util.Optional;

public class OpenAiAPIChatCompletion extends OpenAiApiHandlerBase {

  private final OpenAiApi openAiApi;

  public OpenAiAPIChatCompletion(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public AiSuggestion getOpenAiCompletion(
      AiSuggestionRequest aiSuggestionRequest, List<ChatMessage> chatMessages) {
    return withExceptionHandler(
        () ->
            getChatCompletionFirstChoice(chatMessages)
                .map(choice -> buildAiSuggestion(aiSuggestionRequest, choice))
                .orElse(null));
  }

  private Optional<ChatCompletionChoice> getChatCompletionFirstChoice(
      List<ChatMessage> chatMessages) {
    return openAiApi
        .createChatCompletion(getChatCompletionRequest(chatMessages))
        .blockingGet()
        .getChoices()
        .stream()
        .findFirst();
  }

  private static AiSuggestion buildAiSuggestion(
      AiSuggestionRequest aiSuggestionRequest, ChatCompletionChoice chatCompletionChoice) {
    String content = chatCompletionChoice.getMessage().getContent();
    String incompleteAssistantMessage =
        aiSuggestionRequest.incompleteAssistantMessage == null
            ? ""
            : aiSuggestionRequest.incompleteAssistantMessage;
    String suggestion = incompleteAssistantMessage + content;
    return new AiSuggestion(suggestion, chatCompletionChoice.getFinishReason());
  }

  private static ChatCompletionRequest getChatCompletionRequest(List<ChatMessage> messages) {

    return ChatCompletionRequest.builder()
        .model("gpt-3.5-turbo")
        .messages(messages)
        .n(1)
        // This can go higher (up to 4000 - prompt size), but openAI performance goes down
        // https://help.openai.com/en/articles/4936856-what-are-tokens-and-how-to-count-them
        .maxTokens(200)
        //
        // an effort has been made to make the api call more responsive by using stream(true)
        // however, due to the library limitation, we cannot do it yet.
        // find more details here:
        //    https://github.com/TheoKanning/openai-java/issues/83
        .stream(false)
        .build();
  }
}
