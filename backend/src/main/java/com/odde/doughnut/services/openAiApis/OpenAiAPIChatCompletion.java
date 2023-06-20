package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;

public class OpenAiAPIChatCompletion extends OpenAiApiHandlerBase {

  private final OpenAiApi openAiApi;
  public static final String OPEN_AI_MODEL = "gpt-3.5-turbo";

  public OpenAiAPIChatCompletion(OpenAiApi openAiApi) {
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
              getChatCompletionRequest(context, aiSuggestionRequest);
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

  private static ChatCompletionRequest getChatCompletionRequest(
      String context, AiSuggestionRequest aiSuggestionRequest) {
    List<ChatMessage> messages = new ArrayList<>();
    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), "context: " + context));
    messages.add(new ChatMessage(ChatMessageRole.USER.value(), aiSuggestionRequest.prompt));
    messages.add(
        new ChatMessage(
            ChatMessageRole.ASSISTANT.value(), aiSuggestionRequest.incompleteAssistantMessage));

    return ChatCompletionRequest.builder()
        .model(OPEN_AI_MODEL)
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
