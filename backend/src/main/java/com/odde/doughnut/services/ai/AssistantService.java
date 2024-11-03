package com.odde.doughnut.services.ai;

import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.controllers.dto.AiCompletionRequiredAction;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.assistant.*;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RequiredAction;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.ToolCall;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.List;

public record AssistantService(
    OpenAiApiHandler openAiApiHandler, String assistantId, List<AiTool> tools) {

  public Assistant createDefaultAssistant(String modelName, String assistantName) {
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name(assistantName)
            .instructions(OpenAIChatRequestBuilder.systemInstruction)
            .tools(tools.stream().map(AiTool::getTool).toList())
            .build();
    return openAiApiHandler.createAssistant(assistantRequest);
  }

  public Assistant createAssistantWithFile(
      String modelName, String assistantName, String textContent, String additionalInstruction)
      throws IOException {
    ToolResources tooResources = uploadToolResources(assistantName, textContent);
    List<Tool> toolList = new java.util.ArrayList<>(tools.stream().map(AiTool::getTool).toList());
    toolList.add(new FileSearchTool());
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name(assistantName)
            .toolResources(tooResources)
            .instructions(
                OpenAIChatRequestBuilder.systemInstruction + "\n\n" + additionalInstruction)
            .tools(toolList)
            .build();
    return openAiApiHandler.createAssistant(assistantRequest);
  }

  private ToolResources uploadToolResources(String assistantName, String textContent)
      throws IOException {
    String fileId =
        openAiApiHandler.uploadTextFile(
            assistantName + ".json", textContent, "assistants", ".json");

    String vectorStoreId = openAiApiHandler.createVectorFile(assistantName, fileId);
    FileSearchResources fileSearchResources = new FileSearchResources();
    fileSearchResources.setVectorStoreIds(List.of(vectorStoreId));
    return new ToolResources(null, fileSearchResources);
  }

  public AiAssistantResponse createThreadAndRunWithFirstMessage(Note note, String prompt) {
    String threadId = createThread(note);
    createUserMessage(prompt, threadId);
    Run run = openAiApiHandler.createRun(threadId, assistantId);
    return getThreadResponse(threadId, run);
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

  public String createThread(Note note) {
    ThreadRequest threadRequest =
        ThreadRequest.builder()
            .messages(
                List.of(
                    MessageRequest.builder()
                        .role("assistant")
                        .content(
                            "Please only call the complete_note_details function when use asks to.")
                        .build(),
                    MessageRequest.builder()
                        .role("assistant")
                        .content(note.getNoteDescription())
                        .build()))
            .build();
    return openAiApiHandler.createThread(threadRequest).getId();
  }

  public String createThread() {
    ThreadRequest threadRequest =
        ThreadRequest.builder()
            .messages(
                List.of(
                    MessageRequest.builder()
                        .role("assistant")
                        .content(
                            "Please give us your opinion based on the conversation. If you don't have any opinion, please response you don't have any idea, do not ask me for more information.")
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
      completionResponse.setMessages(openAiApiHandler.getThreadMessages(threadId, id));
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
