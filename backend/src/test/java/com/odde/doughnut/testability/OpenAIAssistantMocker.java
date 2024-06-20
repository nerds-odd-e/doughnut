package com.odde.doughnut.testability;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.assistants.run.*;
import com.theokanning.openai.assistants.thread.Thread;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import java.util.List;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public record OpenAIAssistantMocker(OpenAiApi openAiApi) {
  public OpenAIAssistantThreadMocker mockThreadCreation(String threadId) {
    Thread item = new Thread();
    item.setId(threadId);
    Mockito.doReturn(Single.just(item)).when(openAiApi).createThread(any());
    return new OpenAIAssistantThreadMocker(openAiApi, threadId);
  }

  public void mockThreadRunRequireActionAndCompletionToolCalled(Object result, String runId) {
    Run retrievedRun = getRunThatRequiresAction(result, runId, COMPLETE_NOTE_DETAILS);
    Mockito.doReturn(Single.just(retrievedRun))
        .when(openAiApi)
        .retrieveRun(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  public void mockSubmitOutputAndCompletion(Object result, String runId) {
    Run run = getRunThatRequiresAction(result, runId, COMPLETE_NOTE_DETAILS);
    Mockito.doReturn(Single.just(run)).when(openAiApi).submitToolOutputs(any(), any(), any());
  }

  public void mockSubmitOutputAndRequiredMoreAction(Object result, String runId) {
    Run run = getRunThatRequiresAction(result, runId, askClarificationQuestion);
    Mockito.doReturn(Single.just(run)).when(openAiApi).submitToolOutputs(any(), any(), any());
  }

  private static Run getRunThatRequiresAction(Object result, String runId, String function_name) {
    JsonNode arguments = new ObjectMapper().valueToTree(result);
    Run retrievedRun = new Run();
    retrievedRun.setId(runId);
    retrievedRun.setStatus("requires_action");
    retrievedRun.setRequiredAction(
        RequiredAction.builder()
            .submitToolOutputs(
                SubmitToolOutputs.builder()
                    .toolCalls(
                        List.of(
                            ToolCall.builder()
                                .id("mocked-tool-call-id")
                                .function(
                                    ToolCallFunction.builder()
                                        .name(function_name)
                                        .arguments(arguments)
                                        .build())
                                .build()))
                    .build())
            .build());
    return retrievedRun;
  }
}
