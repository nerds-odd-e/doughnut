package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.services.TriConsumer;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.assistants.run.ToolCall;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.List;
import lombok.Getter;

public class AssistantThread {
  private final String assistantId;
  private final ObjectMapper objectMapper;
  @Getter private String threadId;
  private final OpenAiApiHandler openAiApiHandler;
  private AiTool tool;

  public AssistantThread(String assistantId, String threadId, OpenAiApiHandler openAiApiHandler) {
    this.assistantId = assistantId;
    this.threadId = threadId;
    this.openAiApiHandler = openAiApiHandler;
    this.objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public void createRunForToolCall1(
      TriConsumer<AssistantRunService, String, Object> runServiceAction)
      throws JsonProcessingException {
    Run run = run(tool);
    getToolCallResponse(tool, runServiceAction, run);
  }

  public AssistantThread withTool(AiTool tool) {
    this.tool = tool;
    return this;
  }

  private void getToolCallResponse(
      AiTool tool, TriConsumer<AssistantRunService, String, Object> runServiceAction, Run run)
      throws JsonProcessingException {
    AiAssistantResponse threadResponse = getThreadResponse(threadId, run);
    AssistantRunService runService =
        new AssistantRunService(openAiApiHandler, threadId, threadResponse.getRunId());
    if (runServiceAction != null) {
      Object parsedResponse =
          objectMapper.readValue(
              threadResponse.getToolCalls().getFirst().getFunction().getArguments().toString(),
              tool.parameterClass());
      runServiceAction.accept(
          runService, threadResponse.getToolCalls().getFirst().getId(), parsedResponse);
    }
  }

  private Run run(AiTool tool) {
    RunCreateRequest.RunCreateRequestBuilder builder =
        getCreateRequestBuilder().tools(List.of(tool.getTool()));
    Run run = openAiApiHandler.createRun(threadId, builder.build());
    return run;
  }

  private RunCreateRequest.RunCreateRequestBuilder getCreateRequestBuilder() {
    return RunCreateRequest.builder().assistantId(assistantId);
  }

  private AiAssistantResponse getThreadResponse(String threadId, Run currentRun) {
    String id = currentRun.getId();
    AiAssistantResponse completionResponse = new AiAssistantResponse();
    completionResponse.setThreadId(threadId);
    completionResponse.setRunId(id);

    Run run = openAiApiHandler.retrieveUntilCompletedOrRequiresAction(threadId, currentRun);
    if (run.getStatus().equals("requires_action")) {
      completionResponse.setToolCalls(getAiCompletionRequiredAction(run.getRequiredAction()));
    } else {
      completionResponse.setMessages(openAiApiHandler.getThreadMessages(threadId, id));
    }

    return completionResponse;
  }

  private List<ToolCall> getAiCompletionRequiredAction(RequiredAction requiredAction) {
    int size = requiredAction.getSubmitToolOutputs().getToolCalls().size();
    if (size != 1) {
      throw new RuntimeException("Unexpected number of tool calls: " + size);
    }
    return requiredAction.getSubmitToolOutputs().getToolCalls();
  }

  public OpenAiRunStream runStream() {
    RunCreateRequest.RunCreateRequestBuilder runCreateRequestBuilder = getCreateRequestBuilder();
    Flowable<AssistantSSE> runStream =
        openAiApiHandler.createRunStream(threadId, runCreateRequestBuilder);
    return new OpenAiRunStream(runStream);
  }

  public void createUserMessage(String prompt) {
    MessageRequest messageRequest = MessageRequest.builder().role("user").content(prompt).build();
    openAiApiHandler.createMessage(threadId, messageRequest);
  }

  public void createAssistantMessage(String msg) {
    MessageRequest messageRequest = MessageRequest.builder().role("assistant").content(msg).build();
    openAiApiHandler.createMessage(threadId, messageRequest);
  }
}
