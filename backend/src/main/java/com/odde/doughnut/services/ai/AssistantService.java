package com.odde.doughnut.services.ai;

import com.odde.doughnut.controllers.dto.AiAssistantResponse;
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
    MessageRequest leadingMsg =
        MessageRequest.builder()
            .role("assistant")
            .content("Please only call function to update content when use asks to.")
            .build();
    return createThreadForNoteWithLeadingMessage(note, leadingMsg);
  }

  private String createThreadForNoteWithLeadingMessage(Note note, MessageRequest leadingMsg) {
    ThreadRequest threadRequest =
        ThreadRequest.builder()
            .messages(
                List.of(
                    leadingMsg,
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

  public String suggestTopicTitle(Note note) {
    String threadId =
        createThreadForNoteWithLeadingMessage(
            note,
            MessageRequest.builder()
                .role("assistant")
                .content("Please suggest a topic title for the following note.")
                .build());
    Run run = openAiApiHandler.createRun(threadId, assistantId);
    AiAssistantResponse threadResponse = getThreadResponse(threadId, run);
    openAiApiHandler.cancelRun(threadId, run.getId());
    return threadResponse
        .getToolCalls()
        .getFirst()
        .getFunction()
        .getArguments()
        .get("topic")
        .asText();
  }
}
