package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
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

  public AiSuggestion getOpenAiCompletion(String context, AiSuggestionRequest aiSuggestionRequest) {
    return withExceptionHandler(
        () -> {
          ChatCompletionRequest completionRequest =
              getChatCompletionRequest(context, aiSuggestionRequest.prompt);
          List<ChatCompletionChoice> choices = getChatCompletionChoices(completionRequest);
          return choices.stream()
              .findFirst()
              .map(
                  chatCompletionChoice -> {
                    String content = chatCompletionChoice.getMessage().getContent();
                    String incompleteAssistantMessage =
                        aiSuggestionRequest.incompleteAssistantMessage == null
                            ? ""
                            : aiSuggestionRequest.incompleteAssistantMessage;
                    return new AiSuggestion(
                        incompleteAssistantMessage + content,
                        chatCompletionChoice.getFinishReason());
                  })
              .orElse(null);
        });
  }

  private static ChatCompletionRequest getChatCompletionRequest(String context, String prompt) {
    List<ChatMessage> messages = new ArrayList<>();
    final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value(), prompt);
    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), "context: " + context));
    messages.add(new ChatMessage(ChatMessageRole.USER.value(), "Let's talk"));
    messages.add(systemMessage);

    return ChatCompletionRequest.builder()
        .model(OPEN_AI_MODEL)
        .messages(messages)
        .n(1)
        .maxTokens(200)
        .logitBias(new HashMap<>())
        .build();
  }
}
