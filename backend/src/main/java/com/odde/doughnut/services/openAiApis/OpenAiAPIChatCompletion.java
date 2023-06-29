package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

public class OpenAiAPIChatCompletion extends OpenAiApiHandlerBase {

  private final OpenAiApi openAiApi;

  public OpenAiAPIChatCompletion(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public AiSuggestion getOpenAiCompletion(List<ChatMessage> chatMessages, int maxTokens) {
    return getAiSuggestion(
        defaultChatCompletionRequestBuilder(chatMessages, maxTokens).maxTokens(maxTokens).build());
  }

  private AiSuggestion getAiSuggestion(ChatCompletionRequest request) {
    return withExceptionHandler(
        () ->
            openAiApi.createChatCompletion(request).blockingGet().getChoices().stream()
                .findFirst()
                .map(AiSuggestion::from)
                .orElse(null));
  }

  private static ChatCompletionRequest.ChatCompletionRequestBuilder
      defaultChatCompletionRequestBuilder(List<ChatMessage> messages, int maxTokens) {
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
