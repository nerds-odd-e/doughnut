package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.assistant.Tool;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

public class AssistantThread {
  private final String assistantId;
  @Getter private String threadId;
  private final OpenAiApiHandler openAiApiHandler;
  private final List<AiTool> tools = new ArrayList<>();
  private final List<Tool> mappedTools = new ArrayList<>();
  private String additionalInstructions;
  private String modelName;

  public AssistantThread(
      String assistantId,
      String threadId,
      OpenAiApiHandler openAiApiHandler,
      String additionalInstructions) {
    this.assistantId = assistantId;
    this.threadId = threadId;
    this.openAiApiHandler = openAiApiHandler;
    this.additionalInstructions = additionalInstructions;
  }

  public AssistantThread withTool(AiTool tool) {
    this.tools.add(tool);
    this.mappedTools.add(tool.getTool());
    return this;
  }

  public AssistantThread withAdditionalAdditionalInstructions(String instructions) {
    if (instructions != null && !instructions.isEmpty()) {
      if (additionalInstructions == null) {
        additionalInstructions = instructions;
      } else {
        this.additionalInstructions += "\n\n" + instructions;
      }
    }
    return this;
  }

  public AssistantThread withModelName(String modelName) {
    this.modelName = modelName;
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
    if (!Strings.isEmpty(additionalInstructions)) {
      builder.additionalInstructions(additionalInstructions);
    }
    if (modelName != null) {
      builder.model(modelName);
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
