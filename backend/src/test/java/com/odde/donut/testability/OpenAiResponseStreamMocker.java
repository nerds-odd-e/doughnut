package com.odde.donut.testability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFunctionCallArgumentsDeltaEvent;
import com.openai.models.responses.ResponseFunctionCallArgumentsDoneEvent;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputItemAddedEvent;
import com.openai.models.responses.ResponseStreamEvent;
import com.openai.models.responses.ResponseTextDeltaEvent;
import com.openai.models.responses.ResponseTextDoneEvent;
import com.openai.services.async.ResponseServiceAsync;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

public class OpenAiResponseStreamMocker {
  private final OpenAIClient officialClient;
  private final List<String> messageDeltas = new ArrayList<>();
  private String toolName;
  private List<String> toolArguments = List.of();
  private Runnable beforeEvents;

  public OpenAiResponseStreamMocker withToolCall(String name, String... fragments) {
    toolName = name;
    toolArguments = List.of(fragments);
    return this;
  }

  public OpenAiResponseStreamMocker beforeEvents(Runnable callback) {
    beforeEvents = callback;
    return this;
  }

  public OpenAiResponseStreamMocker(OpenAIClient officialClient) {
    this.officialClient = officialClient;
  }

  public OpenAiResponseStreamMocker withMessage(String message) {
    messageDeltas.add(message);
    return this;
  }

  public void mockStreamResponse() {
    List<ResponseStreamEvent> events = new ArrayList<>();
    long seq = 0;
    for (String part : messageDeltas) {
      events.add(
          ResponseStreamEvent.ofOutputTextDelta(
              ResponseTextDeltaEvent.builder()
                  .delta(part)
                  .itemId("msg-mock")
                  .outputIndex(0L)
                  .contentIndex(0L)
                  .sequenceNumber(++seq)
                  .logprobs(List.of())
                  .build()));
    }
    String fullText = String.join("", messageDeltas);
    events.add(
        ResponseStreamEvent.ofOutputTextDone(
            ResponseTextDoneEvent.builder()
                .text(fullText)
                .itemId("msg-mock")
                .outputIndex(0L)
                .contentIndex(0L)
                .sequenceNumber(++seq)
                .logprobs(List.of())
                .build()));

    if (toolName != null) {
      events.add(
          ResponseStreamEvent.ofOutputItemAdded(
              ResponseOutputItemAddedEvent.builder()
                  .item(
                      ResponseOutputItem.ofFunctionCall(
                          ResponseFunctionToolCall.builder()
                              .id("tool-mock")
                              .callId("call-mock")
                              .name(toolName)
                              .arguments("")
                              .build()))
                  .outputIndex(1L)
                  .sequenceNumber(++seq)
                  .build()));
      for (String part : toolArguments) {
        events.add(
            ResponseStreamEvent.ofFunctionCallArgumentsDelta(
                ResponseFunctionCallArgumentsDeltaEvent.builder()
                    .delta(part)
                    .itemId("tool-mock")
                    .outputIndex(1L)
                    .sequenceNumber(++seq)
                    .build()));
      }
      events.add(
          ResponseStreamEvent.ofFunctionCallArgumentsDone(
              ResponseFunctionCallArgumentsDoneEvent.builder()
                  .arguments(String.join("", toolArguments))
                  .itemId("tool-mock")
                  .outputIndex(1L)
                  .sequenceNumber(++seq)
                  .build()));
    }

    @SuppressWarnings("unchecked")
    AsyncStreamResponse<ResponseStreamEvent> asyncStreamResponse = mock(AsyncStreamResponse.class);

    CompletableFuture<Void> completionFuture = CompletableFuture.completedFuture(null);
    when(asyncStreamResponse.onCompleteFuture()).thenReturn(completionFuture);

    Answer<AsyncStreamResponse<ResponseStreamEvent>> subscribeAnswer =
        invocation -> {
          AsyncStreamResponse.Handler<ResponseStreamEvent> handler = invocation.getArgument(0);
          if (beforeEvents != null) beforeEvents.run();
          for (ResponseStreamEvent event : events) {
            handler.onNext(event);
          }
          handler.onComplete(Optional.empty());
          return asyncStreamResponse;
        };

    when(asyncStreamResponse.subscribe(
            ArgumentMatchers.<AsyncStreamResponse.Handler<ResponseStreamEvent>>any()))
        .thenAnswer(subscribeAnswer);
    when(asyncStreamResponse.subscribe(
            ArgumentMatchers.<AsyncStreamResponse.Handler<ResponseStreamEvent>>any(),
            any(Executor.class)))
        .thenAnswer(subscribeAnswer);

    ResponseServiceAsync responseService = mock(ResponseServiceAsync.class);
    when(responseService.createStreaming(any(ResponseCreateParams.class)))
        .thenReturn(asyncStreamResponse);

    OpenAIClientAsync asyncClient = mock(OpenAIClientAsync.class);
    when(asyncClient.responses()).thenReturn(responseService);

    when(officialClient.async()).thenReturn(asyncClient);
  }
}
