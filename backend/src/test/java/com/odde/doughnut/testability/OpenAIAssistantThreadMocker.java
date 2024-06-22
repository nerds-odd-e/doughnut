package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.assistants.StreamEvent;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.run.*;
import com.theokanning.openai.assistants.run_step.RunStep;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.mockito.Mockito;
import retrofit2.Call;

public record OpenAIAssistantThreadMocker(OpenAiApi openAiApi, String threadId) {
  public OpenAIAssistantThreadMocker mockCreateMessage() {
    when(openAiApi.createMessage(eq(threadId), any())).thenReturn(Single.just(new Message()));
    return this;
  }

  public OpenAIAssistantCreatedRunMocker mockCreateRunInProcess(String runId) {
    Run run = getRun(runId, "processing");
    Mockito.doReturn(Single.just(run)).when(openAiApi).createRun(any(), any());
    return new OpenAIAssistantCreatedRunMocker(openAiApi, threadId, runId);
  }

  private Run getRun(String runId, String status) {
    Run run = new Run();
    run.setId(runId);
    run.setStatus(status);
    run.setThreadId(threadId);
    return run;
  }

  public OpenAIAssistantRunCompletedMocker mockCreateRunStream(String runId) {
    RunStep runStep = RunStep.builder().id("runStepId").runId(runId).status("completed").build();
    String data = null;
    try {
      data = new ObjectMapper().writeValueAsString(runStep);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    AssistantSSE assistantSSE = new AssistantSSE(StreamEvent.THREAD_RUN_STEP_COMPLETED, data);
    String assistantSSEString =
        "event: "
            + assistantSSE.getEvent().eventName
            + "\n"
            + "data: "
            + assistantSSE.getData()
            + "\n\n";
    ResponseBody responseBody =
        ResponseBody.create(assistantSSEString, MediaType.parse("text/event-stream"));
    Call<ResponseBody> call = new ResponseBodyCallStub(responseBody);

    Mockito.doReturn(call).when(openAiApi).createRunStream(any(), any());
    return new OpenAIAssistantRunCompletedMocker(openAiApi, threadId, getRun(runId, "completed"));
  }
}
