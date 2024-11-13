package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class AssistantService {
  private final OpenAiApiHandler openAiApiHandler;
  private final String assistantId;

  public AssistantService(OpenAiApiHandler openAiApiHandler, String assistantId) {
    this.openAiApiHandler = openAiApiHandler;
    this.assistantId = assistantId;
  }

  public Flowable<AssistantSSE> getRunStream(String threadId) {
    return openAiApiHandler.createRunStream(threadId, assistantId);
  }

  public void createUserMessage(String prompt, String threadId) {
    MessageRequest messageRequest = MessageRequest.builder().role("user").content(prompt).build();
    openAiApiHandler.createMessage(threadId, messageRequest);
  }

  public void createAssistantMessage(String msg, String threadId) {
    MessageRequest messageRequest = MessageRequest.builder().role("assistant").content(msg).build();
    openAiApiHandler.createMessage(threadId, messageRequest);
  }

  public String createThread(List<MessageRequest> additionalMessages) {
    List<MessageRequest> messages =
        new ArrayList<>(
            List.of(
                MessageRequest.builder()
                    .role("assistant")
                    .content("Please only call function to update content when user asks to.")
                    .build()));

    if (additionalMessages != null && !additionalMessages.isEmpty()) {
      messages.addAll(additionalMessages);
    }
    ThreadRequest threadRequest = ThreadRequest.builder().messages(messages).build();
    return openAiApiHandler.createThread(threadRequest).getId();
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

  public AiAssistantResponse createRunAndGetThreadResponse(String threadId) {
    Run run = openAiApiHandler.createRun(threadId, assistantId);
    return getThreadResponse(threadId, run);
  }

  public AssistantRunService getAssistantRunService(String threadId, String runId) {
    return new AssistantRunService(openAiApiHandler, threadId, runId);
  }

  public SseEmitter getRunStreamAsSSE(Consumer<Message> messageConsumer, String threadId1) {
    Flowable<AssistantSSE> runStream = getRunStream(threadId1);
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
