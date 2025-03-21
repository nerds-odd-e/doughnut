package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.assistants.StreamEvent;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.MessageContent;
import com.theokanning.openai.assistants.message.content.Delta;
import com.theokanning.openai.assistants.message.content.DeltaContent;
import com.theokanning.openai.assistants.message.content.MessageDelta;
import com.theokanning.openai.assistants.message.content.Text;
import com.theokanning.openai.assistants.run_step.RunStep;
import com.theokanning.openai.client.OpenAiApi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.mockito.Mockito;
import retrofit2.Call;

public final class OpenAIAssistantRunStreamMocker {
  private final OpenAiApi openAiApi;
  private final String runId;
  List<MessageDelta> messageDeltas = new ArrayList<>();
  private Message completedMessage = null;

  public OpenAIAssistantRunStreamMocker(OpenAiApi openAiApi, String runId) {
    this.openAiApi = openAiApi;
    this.runId = runId;
  }

  public OpenAIAssistantRunStreamMocker withMessageDeltas(String... args) {
    Arrays.stream(args)
        .map(
            msg -> {
              Text text = new Text(msg, List.of());
              List<DeltaContent> content = List.of(new DeltaContent(0, "text", text, null, null));
              Delta delta = new Delta("assistant", content);
              return new MessageDelta("fake-id", "thread.message.delta", delta);
            })
        .forEach(messageDeltas::add);
    return this;
  }

  public void mockTheRunStream() {
    RunStep runStep = RunStep.builder().id("runStepId").runId(runId).status("completed").build();
    String assistantSSEString =
        Stream.concat(
                messageDeltas.stream()
                    .map(delta -> toSSEString(StreamEvent.THREAD_MESSAGE_DELTA.eventName, delta)),
                Stream.concat(
                    completedMessage != null
                        ? Stream.of(
                            toSSEString(
                                StreamEvent.THREAD_MESSAGE_COMPLETED.eventName, completedMessage))
                        : Stream.empty(),
                    Stream.of(
                        toSSEString(StreamEvent.THREAD_RUN_STEP_COMPLETED.eventName, runStep),
                        toSSEString("done", "DONE"))))
            .collect(Collectors.joining());
    ResponseBody responseBody =
        ResponseBody.create(MediaType.parse("text/event-stream"), assistantSSEString);
    Call<ResponseBody> call = new ResponseBodyCallStub(responseBody);

    Mockito.doReturn(call).when(openAiApi).createRunStream(any(), any());
  }

  private static String toSSEString(String streamEvent, Object dataObject) {
    String data = null;
    try {
      data = new ObjectMapper().writeValueAsString(dataObject);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return "event: " + streamEvent + "\n" + "data: " + data + "\n\n";
  }

  public OpenAIAssistantRunStreamMocker withMessageCompleted(String message) {
    Text text = new Text(message, List.of());
    MessageContent msgCnt = new MessageContent();
    msgCnt.setType("text");
    msgCnt.setText(text);
    List<MessageContent> content = List.of(msgCnt);
    completedMessage = Message.builder().role("assistant").content(content).build();
    return this;
  }
}
