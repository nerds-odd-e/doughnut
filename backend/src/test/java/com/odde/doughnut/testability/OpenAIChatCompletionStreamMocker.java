package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.services.GlobalSettingsService;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.services.async.ChatServiceAsync;
import com.openai.services.async.chat.ChatCompletionServiceAsync;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

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
              .model(GlobalSettingsService.DEFAULT_CHAT_MODEL)
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
            .model(GlobalSettingsService.DEFAULT_CHAT_MODEL)
            .choices(List.of(finalChoice))
            .build();
    chunks.add(finalChunk);

    // Mock the async streaming response
    @SuppressWarnings("unchecked")
    AsyncStreamResponse<ChatCompletionChunk> asyncStreamResponse = mock(AsyncStreamResponse.class);

    CompletableFuture<Void> completionFuture = CompletableFuture.completedFuture(null);
    when(asyncStreamResponse.onCompleteFuture()).thenReturn(completionFuture);

    Answer<AsyncStreamResponse<ChatCompletionChunk>> subscribeAnswer =
        invocation -> {
          AsyncStreamResponse.Handler<ChatCompletionChunk> handler = invocation.getArgument(0);
          for (ChatCompletionChunk chunk : chunks) {
            handler.onNext(chunk);
          }
          handler.onComplete(Optional.empty());
          return asyncStreamResponse;
        };

    when(asyncStreamResponse.subscribe(
            ArgumentMatchers.<AsyncStreamResponse.Handler<ChatCompletionChunk>>any()))
        .thenAnswer(subscribeAnswer);
    when(asyncStreamResponse.subscribe(
            ArgumentMatchers.<AsyncStreamResponse.Handler<ChatCompletionChunk>>any(),
            any(Executor.class)))
        .thenAnswer(subscribeAnswer);

    ChatCompletionServiceAsync completionService = mock(ChatCompletionServiceAsync.class);
    when(completionService.createStreaming(any(ChatCompletionCreateParams.class)))
        .thenReturn(asyncStreamResponse);

    ChatServiceAsync chatService = mock(ChatServiceAsync.class);
    when(chatService.completions()).thenReturn(completionService);

    OpenAIClientAsync asyncClient = mock(OpenAIClientAsync.class);
    when(asyncClient.chat()).thenReturn(chatService);

    when(officialClient.async()).thenReturn(asyncClient);
  }
}
