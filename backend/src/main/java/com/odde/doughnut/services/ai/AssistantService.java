package com.odde.doughnut.services.ai;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.SettingAccessor;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.assistant.AssistantRequest;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import java.sql.Timestamp;
import java.util.List;

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

  public AiAssistantResponse createThreadAndRunWithFirstMessageForChat(Note note, String prompt) {
    String threadId = createThread(note);
    return createMessageRunAndGetResponseForChat(prompt, threadId);
  }

  public AiAssistantResponse createMessageRunAndGetResponseForChat(String prompt, String threadId) {
    MessageRequest messageRequest = MessageRequest.builder().role("user").content(prompt).build();
    openAiApiHandler.createMessage(threadId, messageRequest);
    Run run = openAiApiHandler.createRun(threadId, settingAccessor.getValue());
    return getThreadResponse(threadId, run);
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
