package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.List;
import lombok.Getter;

public class AssistantThread {
  private final String assistantId;
  @Getter private String threadId;
  private final OpenAiApiHandler openAiApiHandler;
  private AiTool tool;
  private String instructions;

  public AssistantThread(String assistantId, String threadId, OpenAiApiHandler openAiApiHandler) {
    this.assistantId = assistantId;
    this.threadId = threadId;
    this.openAiApiHandler = openAiApiHandler;
  }

  public AssistantThread withTool(AiTool tool) {
    this.tool = tool;
    return this;
  }

  public AssistantThread withInstructions(String instructions) {
    this.instructions = instructions;
    return this;
  }

  public OpenAiRun run() {
    RunCreateRequest.RunCreateRequestBuilder builder =
        getCreateRequestBuilder().tools(List.of(tool.getTool()));
    return new OpenAiRun(
        openAiApiHandler, threadId, openAiApiHandler.createRun(threadId, builder.build()), tool);
  }

  private RunCreateRequest.RunCreateRequestBuilder getCreateRequestBuilder() {
    RunCreateRequest.RunCreateRequestBuilder builder =
        RunCreateRequest.builder().assistantId(assistantId);
    if (instructions != null) {
      builder.instructions(instructions);
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
}
