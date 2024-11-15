package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.services.TriConsumer;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.assistants.run.ToolCall;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class AssistantThread {
  public final String assistantId;
  private final ObjectMapper objectMapper;
  public String threadId;
  private final OpenAiApiHandler openAiApiHandler;

  public AssistantThread(String assistantId, String threadId, OpenAiApiHandler openAiApiHandler) {
    this.assistantId = assistantId;
    this.threadId = threadId;
    this.openAiApiHandler = openAiApiHandler;
    this.objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public void createThreadAndRunForToolCall(
      AiTool tool, TriConsumer<AssistantRunService, String, Object> runServiceAction)
      throws JsonProcessingException {
    RunCreateRequest.RunCreateRequestBuilder builder =
        RunCreateRequest.builder().tools(List.of(tool.getTool()));
    Run run = openAiApiHandler.createRun(threadId, builder.assistantId(this.assistantId).build());
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

  public SseEmitter getRunStreamAsSSE(Consumer<Message> messageConsumer) {
    Flowable<AssistantSSE> runStream = openAiApiHandler.createRunStream(threadId, assistantId);
    SseEmitter emitter = new SseEmitter();
    runStream.subscribe(
        sse -> {
          try {
            SseEmitter.SseEventBuilder builder =
                SseEmitter.event().name(sse.getEvent().eventName).data(sse.getData());
            emitter.send(builder);

            // Handle thread.message.completed event
            if (Objects.equals(sse.getEvent().eventName, "thread.message.completed")) {
              Message message = new ObjectMapper().readValue(sse.getData(), Message.class);
              if (messageConsumer != null) {
                messageConsumer.accept(message);
              }
            }

            if (Objects.equals(sse.getEvent().eventName, "done")) {
              emitter.complete();
            }
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        });
    return emitter;
  }
}
