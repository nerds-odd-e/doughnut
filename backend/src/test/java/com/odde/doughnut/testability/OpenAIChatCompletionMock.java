package com.odde.doughnut.testability;

import com.odde.doughnut.configs.ObjectMapperConfig;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.services.blocking.ChatService;
import com.openai.services.blocking.chat.ChatCompletionService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class OpenAIChatCompletionMock {
  private final ChatCompletionService completionService;
  private final ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();

  public OpenAIChatCompletionMock(OpenAIClient officialClient) {
    ChatService chatService = Mockito.mock(ChatService.class);
    this.completionService = Mockito.mock(ChatCompletionService.class);
    Mockito.when(officialClient.chat()).thenReturn(chatService);
    Mockito.when(chatService.completions()).thenReturn(completionService);
  }

  public ChatCompletionService completionService() {
    return completionService;
  }

  public void mockChatCompletionAndReturnJsonSchema(Object result) {
    if (result == null) {
      mockNullChatCompletion();
      return;
    }
    String content = objectMapperConfig.objectMapper().valueToTree(result).toString();
    ChatCompletion completion = buildContentCompletion(content);
    Mockito.doReturn(completion)
        .when(completionService)
        .create(ArgumentMatchers.argThat((ChatCompletionCreateParams params) -> !hasTools(params)));
  }

  public void mockNullChatCompletion() {
    ChatCompletion completion =
        ChatCompletion.builder()
            .id("chatcmpl-null")
            .created(System.currentTimeMillis() / 1000L)
            .model("gpt-4o-mini")
            .choices(Collections.emptyList())
            .build();
    Mockito.doReturn(completion)
        .when(completionService)
        .create(ArgumentMatchers.argThat((ChatCompletionCreateParams params) -> !hasTools(params)));
  }

  public void mockChatCompletionWithMalformedJsonContent(String malformedJson) {
    ChatCompletion completion = buildContentCompletion(malformedJson);
    Mockito.doReturn(completion)
        .when(completionService)
        .create(ArgumentMatchers.argThat((ChatCompletionCreateParams params) -> !hasTools(params)));
  }

  private boolean hasTools(ChatCompletionCreateParams params) {
    return params.tools().map(list -> !list.isEmpty()).orElse(false);
  }

  private ChatCompletion buildContentCompletion(String content) {
    ChatCompletionMessage message =
        ChatCompletionMessage.builder()
            .role(JsonValue.from("assistant"))
            .content(content)
            .refusal(Optional.empty())
            .build();
    ChatCompletion.Choice choice =
        ChatCompletion.Choice.builder()
            .index(0L)
            .message(message)
            .finishReason(ChatCompletion.Choice.FinishReason.STOP)
            .logprobs(Optional.empty())
            .build();
    return ChatCompletion.builder()
        .id("chatcmpl-mock")
        .created(System.currentTimeMillis() / 1000L)
        .model("gpt-4o-mini")
        .choices(List.of(choice))
        .build();
  }
}
