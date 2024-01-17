package com.odde.doughnut.services.ai;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;

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
import com.theokanning.openai.threads.Thread;
import com.theokanning.openai.threads.ThreadRequest;
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

    if (!run.getStatus().equals("requires_action")) {
      throw new RuntimeException("not implemented");
    }
    RequiredAction requiredAction = run.getRequiredAction();
    ToolCall toolCall = requiredAction.getSubmitToolOutputs().getToolCalls().get(0);

    AiCompletionRequiredAction actionRequired =
        getTools()
            .flatMap(t -> t.tryConsume(toolCall))
            .findFirst()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Unknown function name: " + toolCall.getFunction().getName()));

    actionRequired.setToolCallId(toolCall.getId());

    if (actionRequired.getMoreCompleteContent() != null) {
      actionRequired.setMoreCompleteContent(
          detailsToComplete + actionRequired.getMoreCompleteContent());
    }
    AiCompletionResponse completionResponseForClarification = new AiCompletionResponse();
    completionResponseForClarification.setRequiredAction(actionRequired);
    completionResponseForClarification.setThreadId(threadId);
    completionResponseForClarification.setRunId(currentRun.getId());
    return completionResponseForClarification;
  }

  public Assistant createNoteCompletionAssistant(String modelName) {
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name("Note details completion")
            .instructions(OpenAIChatRequestBuilder.systemInstruction)
            .tools(getTools().map(AiTool::getTool).toList())
            .build();
    return openAiApiHandler.createAssistant(assistantRequest);
  }

  private static Stream<AiTool> getTools() {
    return Stream.of(
        AiTool.build(
            COMPLETE_NOTE_DETAILS,
            "Text completion for the details of the note of focus",
            NoteDetailsCompletion.class,
            (noteDetailsCompletion) -> {
              AiCompletionRequiredAction result = new AiCompletionRequiredAction();
              result.setMoreCompleteContent(noteDetailsCompletion.completion);
              return result;
            }),
        AiTool.build(
            askClarificationQuestion,
            "Ask question to get more context",
            ClarifyingQuestion.class,
            (clarifyingQuestion) -> {
              AiCompletionRequiredAction result = new AiCompletionRequiredAction();
              result.setClarifyingQuestion(clarifyingQuestion);
              return result;
            }));
  }
}
