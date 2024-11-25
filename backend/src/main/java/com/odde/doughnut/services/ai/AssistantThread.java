package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.assistant.FileSearchTool;
import com.theokanning.openai.assistants.assistant.Tool;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class AssistantThread {
  private final String assistantId;
  @Getter private String threadId;
  private final OpenAiApiHandler openAiApiHandler;
  private final List<AiTool> tools = new ArrayList<>();
  private final List<Tool> mappedTools = new ArrayList<>();
  private String additionalInstructions;

  public AssistantThread(String assistantId, String threadId, OpenAiApiHandler openAiApiHandler) {
    this.assistantId = assistantId;
    this.threadId = threadId;
    this.openAiApiHandler = openAiApiHandler;
  }

  public AssistantThread withTool(AiTool tool) {
    this.tools.add(tool);
    this.mappedTools.add(tool.getTool());
    return this;
  }

  public AssistantThread withFileSearch() {
    this.mappedTools.add(new FileSearchTool());
    return this;
  }

  public AssistantThread withAdditionalInstructions(String instructions) {
    this.additionalInstructions = instructions;
    return this;
  }

  public OpenAiRunExpectingAction run() {
    RunCreateRequest.RunCreateRequestBuilder builder = getCreateRequestBuilder().tools(mappedTools);
    return new OpenAiRunExpectingAction(
        openAiApiHandler, openAiApiHandler.createRun(threadId, builder.build()), tools.get(0));
  }

  private RunCreateRequest.RunCreateRequestBuilder getCreateRequestBuilder() {
    RunCreateRequest.RunCreateRequestBuilder builder =
        RunCreateRequest.builder().assistantId(assistantId);
    if (additionalInstructions != null) {
      builder.additionalInstructions(additionalInstructions);
    }
    return builder;
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

  public OpenAiRun resumeRun(String runId) {
    return new OpenAiRunResumed(openAiApiHandler, threadId, runId, tools.get(0));
  }
}
