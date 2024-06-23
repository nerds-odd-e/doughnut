package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.SettingAccessor;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.StreamEvent;
import com.theokanning.openai.assistants.assistant.AssistantRequest;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.MessageContent;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.message.content.Delta;
import com.theokanning.openai.assistants.message.content.DeltaContent;
import com.theokanning.openai.assistants.message.content.MessageDelta;
import com.theokanning.openai.assistants.message.content.Text;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;
import com.theokanning.openai.assistants.run_step.RunStep;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.subscribers.TestSubscriber;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record AssistantService(
    OpenAiApiHandler openAiApiHandler,
    SettingAccessor settingAccessor,
    String assistantName,
    List<AiTool> tools) {

  public String createAssistant(String modelName, Timestamp currentUTCTimestamp) {
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name(assistantName)
            .instructions(OpenAIChatRequestBuilder.systemInstruction)
            .tools(tools.stream().map(AiTool::getTool).toList())
            .build();
    String chatAssistant = openAiApiHandler.createAssistant(assistantRequest).getId();
    settingAccessor.setKeyValue(currentUTCTimestamp, chatAssistant);
    return chatAssistant;
  }

  public AiAssistantResponse createThreadAndRunWithFirstMessage(Note note, String prompt) {
    String threadId = createThread(note);
    MessageRequest messageRequest = MessageRequest.builder().role("user").content(prompt).build();
    openAiApiHandler.createMessage(threadId, messageRequest);
    Run run = openAiApiHandler.createRun(threadId, settingAccessor.getValue());
    return getThreadResponse(threadId, run);
  }

  public AiAssistantResponse createThreadAndRunWithFirstMessageStream(Note note, String prompt) {
    String threadId = createThread(note);
    return createMessageRunAndGetResponseStream(prompt, threadId);
  }

  public AiAssistantResponse createMessageRunAndGetResponseStream(String prompt, String threadId) {
    MessageRequest messageRequest = MessageRequest.builder().role("user").content(prompt).build();
    openAiApiHandler.createMessage(threadId, messageRequest);
    TestSubscriber<AssistantSSE> subscriber = new TestSubscriber<>();
    openAiApiHandler
        .createRunStream(threadId, settingAccessor.getValue())
        .blockingSubscribe(subscriber);
    //    System.out.println(subscriber.getEvents());
    Optional<AssistantSSE> runStepCompletion =
        subscriber.values().stream()
            .filter(item -> item.getEvent().equals(StreamEvent.THREAD_RUN_STEP_COMPLETED))
            .findFirst();
    RunStep runStep = null;
    try {
      runStep = new ObjectMapper().readValue(runStepCompletion.get().getData(), RunStep.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    String reply =
        subscriber.values().stream()
            .filter(item -> item.getEvent().equals(StreamEvent.THREAD_MESSAGE_DELTA))
            .map(
                item -> {
                  try {
                    return new ObjectMapper().readValue(item.getData(), MessageDelta.class);
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                  }
                })
            .map(MessageDelta::getDelta)
            .map(Delta::getContent)
            .map(List::getFirst)
            .map(DeltaContent::getText)
            .map(Text::getValue)
            .collect(Collectors.joining());

    MessageContent cnt = new MessageContent();
    cnt.setText(new Text(reply, List.of()));
    Message message =
        Message.builder().threadId(threadId).role("assistant").content(List.of(cnt)).build();

    AiAssistantResponse completionResponse = new AiAssistantResponse();
    completionResponse.setThreadId(threadId);
    completionResponse.setRunId(runStep.getRunId());
    completionResponse.setMessages(List.of(message));
    return completionResponse;
  }

  public AiAssistantResponse answerAiCompletionClarifyingQuestion(
      AiCompletionAnswerClarifyingQuestionParams answerClarifyingQuestionParams) {
    String threadId = answerClarifyingQuestionParams.getThreadId();

    Run retrievedRun = openAiApiHandler.submitToolOutputs(answerClarifyingQuestionParams);

    return getThreadResponse(threadId, retrievedRun);
  }

  private String createThread(Note note) {
    ThreadRequest threadRequest =
        ThreadRequest.builder()
            .messages(
                List.of(
                    MessageRequest.builder()
                        .role("assistant")
                        .content(note.getNoteDescription())
                        .build()))
            .build();
    return openAiApiHandler.createThread(threadRequest).getId();
  }

  private AiAssistantResponse getThreadResponse(String threadId, Run currentRun) {
    String id = currentRun.getId();
    AiAssistantResponse completionResponse = new AiAssistantResponse();
    completionResponse.setThreadId(threadId);
    completionResponse.setRunId(id);

    Run run = openAiApiHandler.retrieveUntilCompletedOrRequiresAction(threadId, currentRun);
    if (run.getStatus().equals("requires_action")) {
      completionResponse.setRequiredAction(getAiCompletionRequiredAction(run.getRequiredAction()));
    } else {
      completionResponse.setMessages(openAiApiHandler.getThreadLastMessage(threadId, id));
    }

    return completionResponse;
  }

  private AiCompletionRequiredAction getAiCompletionRequiredAction(RequiredAction requiredAction) {
    int size = requiredAction.getSubmitToolOutputs().getToolCalls().size();
    if (size != 1) {
      throw new RuntimeException("Unexpected number of tool calls: " + size);
    }
    ToolCall toolCall = requiredAction.getSubmitToolOutputs().getToolCalls().getFirst();

    AiCompletionRequiredAction actionRequired =
        tools.stream()
            .flatMap(t -> t.tryConsume(toolCall))
            .findFirst()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Unknown function name: " + toolCall.getFunction().getName()));

    actionRequired.setToolCallId(toolCall.getId());
    return actionRequired;
  }
}
