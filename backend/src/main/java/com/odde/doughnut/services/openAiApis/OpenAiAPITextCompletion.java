package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpenAiAPITextCompletion extends OpenAiApiHandlerBase {

  private final OpenAiApi openAiApi;
  public static final String OPEN_AI_MODEL = "gpt-3.5-turbo";

  public OpenAiAPITextCompletion(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  private List<ChatCompletionChoice> getChatCompletionChoices(
      ChatCompletionRequest completionRequest) {
    return openAiApi.createChatCompletion(completionRequest).blockingGet().getChoices();
  }

  public AiSuggestion getOpenAiCompletion(String prompt) {

    return withExceptionHandler(
        () -> {
          ChatCompletionRequest completionRequest = getChatCompletionRequest(prompt);
          List<ChatCompletionChoice> choices = getChatCompletionChoices(completionRequest);
          return choices.stream()
              .findFirst()
              .map(
                  chatCompletionChoice ->
                      new AiSuggestion(
                          chatCompletionChoice.getMessage().getContent(),
                          chatCompletionChoice.getFinishReason()))
              .orElse(null);
        });
  }

  private static ChatCompletionRequest getChatCompletionRequest(String prompt) {
    List<ChatMessage> messages = new ArrayList<>();
    final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);
    messages.add(0, systemMessage);

    return ChatCompletionRequest.builder()
        .model(OPEN_AI_MODEL)
        .messages(messages)
        .n(1)
        .maxTokens(50)
        .logitBias(new HashMap<>())
        .build();
  }
}
