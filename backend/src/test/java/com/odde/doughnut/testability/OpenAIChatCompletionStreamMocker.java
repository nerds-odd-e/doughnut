package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OpenAIChatCompletionStreamMocker {
  private final OpenAIClient officialClient;
  private final List<String> messageDeltas = new ArrayList<>();

  public OpenAIChatCompletionStreamMocker(OpenAIClient officialClient) {
    this.officialClient = officialClient;
  }

  public OpenAIChatCompletionStreamMocker withMessage(String message) {
    // Split message into chunks for streaming
    messageDeltas.add(message);
    return this;
  }

  public void mockStreamResponse() {
    // Build list of ChatCompletionChunk objects
    List<ChatCompletionChunk> chunks = new ArrayList<>();

    // Add chunks for each message
    for (String message : messageDeltas) {
      Delta delta =
          Delta.builder()
              .content(message)
              .role(ChatCompletionChunk.Choice.Delta.Role.ASSISTANT)
              .build();
      Choice choice =
          Choice.builder()
              .index(0L)
              .delta(delta)
              .finishReason(Optional.empty())
              .logprobs(Optional.empty())
              .build();
      ChatCompletionChunk chunk =
          ChatCompletionChunk.builder()
              .id("chatcmpl-mock")
              .created(System.currentTimeMillis() / 1000L)
              .model("gpt-4o-mini")
              .choices(List.of(choice))
              .build();
      chunks.add(chunk);
    }

    // Add final chunk with finish reason
    Delta finalDelta = Delta.builder().build();
    Choice finalChoice =
        Choice.builder()
            .index(0L)
            .delta(finalDelta)
            .finishReason(Optional.of(ChatCompletionChunk.Choice.FinishReason.STOP))
            .logprobs(Optional.empty())
            .build();
    ChatCompletionChunk finalChunk =
        ChatCompletionChunk.builder()
            .id("chatcmpl-mock")
            .created(System.currentTimeMillis() / 1000L)
            .model("gpt-4o-mini")
            .choices(List.of(finalChoice))
            .build();
    chunks.add(finalChunk);

    // Mock the streaming response
    @SuppressWarnings("unchecked")
    StreamResponse<ChatCompletionChunk> streamResponse = mock(StreamResponse.class);
    when(streamResponse.stream()).thenReturn(chunks.stream());

    com.openai.services.blocking.chat.ChatCompletionService completionService =
        mock(com.openai.services.blocking.chat.ChatCompletionService.class);
    when(completionService.createStreaming(any(ChatCompletionCreateParams.class)))
        .thenReturn(streamResponse);

    com.openai.services.blocking.ChatService chatService =
        mock(com.openai.services.blocking.ChatService.class);
    when(chatService.completions()).thenReturn(completionService);

    when(officialClient.chat()).thenReturn(chatService);
  }
}
