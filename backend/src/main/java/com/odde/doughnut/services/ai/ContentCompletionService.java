package com.odde.doughnut.services.ai;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.*;
import com.theokanning.openai.messages.MessageRequest;
import com.theokanning.openai.runs.RequiredAction;
import com.theokanning.openai.runs.Run;
import com.theokanning.openai.runs.ToolCall;
import com.theokanning.openai.runs.ToolCallFunction;
import com.theokanning.openai.threads.Thread;
import com.theokanning.openai.threads.ThreadRequest;
import java.util.List;
import java.util.stream.Stream;

public record ContentCompletionService(OpenAiApiHandler openAiApiHandler) {
  public AiCompletionResponse getAiCompletion(
      AiCompletionParams aiCompletionParams, Note note, String assistantId) {
    String threadId = createThread(aiCompletionParams, note);
    Run run = openAiApiHandler.createRun(threadId, assistantId);
    return getThreadResponse(threadId, aiCompletionParams.getDetailsToComplete(), run);
  }

  public AiCompletionResponse answerAiCompletionClarifyingQuestion(
      AiCompletionAnswerClarifyingQuestionParams answerClarifyingQuestionParams) {
    String threadId = answerClarifyingQuestionParams.getThreadId();

    Run retrievedRun = openAiApiHandler.submitToolOutputs(answerClarifyingQuestionParams);

    return getThreadResponse(
        threadId, answerClarifyingQuestionParams.getDetailsToComplete(), retrievedRun);
  }

  private String createThread(AiCompletionParams aiCompletionParams, Note note) {
    ThreadRequest threadRequest = ThreadRequest.builder().build();
    Thread thread = openAiApiHandler.createThread(threadRequest);
    MessageRequest messageRequest =
        MessageRequest.builder()
            .content(
                note.getNoteDescription()
                    + "------------\n"
                    + aiCompletionParams.getCompletionPrompt())
            .build();

    openAiApiHandler.createMessage(thread.getId(), messageRequest);
    return thread.getId();
  }

  private AiCompletionResponse getThreadResponse(
      String threadId, String detailsToComplete, Run currentRun) {
    Run run = openAiApiHandler.retrieveUntilCompletedOrRequiresAction(threadId, currentRun);

    AiCompletionResponse completionResponseForClarification;
    if (run.getStatus().equals("requires_action")) {
      RequiredAction requiredAction = run.getRequiredAction();
      ToolCall toolCall = requiredAction.getSubmitToolOutputs().getToolCalls().get(0);
      ToolCallFunction function = toolCall.getFunction();
      String arguments = function.getArguments();
      JsonNode jsonNode = null;
      try {
        jsonNode = defaultObjectMapper().readTree(arguments);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }

      if (function.getName().equals(askClarificationQuestion)) {
        ClarifyingQuestion result1;
        try {
          result1 = defaultObjectMapper().treeToValue(jsonNode, ClarifyingQuestion.class);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
        AiCompletionResponse result = new AiCompletionResponse();
        ClarifyingQuestionRequiredAction cqra = new ClarifyingQuestionRequiredAction();
        cqra.clarifyingQuestion = result1;
        cqra.toolCallId = toolCall.getId();

        result.setClarifyingQuestionRequiredAction(cqra);
        completionResponseForClarification = result;
      } else if (function.getName().equals(COMPLETE_NOTE_DETAILS)) {
        completionResponseForClarification = getAiCompletionResponse(jsonNode, detailsToComplete);
      } else {
        throw new RuntimeException("Unknown function name: " + function.getName());
      }
    } else {
      throw new RuntimeException("not implemented");
    }
    completionResponseForClarification.setThreadId(threadId);
    completionResponseForClarification.setRunId(currentRun.getId());
    return completionResponseForClarification;
  }

  public Assistant createNoteCompletionAssistant(String modelName) {
    List<Tool> toolList = getTools().map(AiTool::getTool).toList();
    return createAssistant(modelName, toolList);
  }

  private static Stream<AiTool> getTools() {
    return Stream.of(
        new AiTool(
            COMPLETE_NOTE_DETAILS,
            "Text completion for the details of the note of focus",
            NoteDetailsCompletion.class),
        new AiTool(
            askClarificationQuestion,
            "Ask question to get more context",
            ClarifyingQuestion.class));
  }

  private Assistant createAssistant(String modelName, List<Tool> tools) {
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name("Note details completion")
            .instructions(OpenAIChatRequestBuilder.systemInstruction)
            .tools(tools)
            .build();
    return openAiApiHandler.createAssistant(assistantRequest);
  }

  private static AiCompletionResponse getAiCompletionResponse(
      JsonNode jsonNode, String detailsToComplete) {
    String result1;
    try {
      NoteDetailsCompletion noteDetailsCompletion =
          defaultObjectMapper().treeToValue(jsonNode, NoteDetailsCompletion.class);
      result1 = detailsToComplete + noteDetailsCompletion.completion;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    AiCompletionResponse result = new AiCompletionResponse();
    String content = result1;
    result.setMoreCompleteContent(content);
    return result;
  }
}
