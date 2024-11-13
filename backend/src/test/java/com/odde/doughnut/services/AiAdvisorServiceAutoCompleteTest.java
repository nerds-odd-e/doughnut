package com.odde.doughnut.services;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.services.ai.AssistantService;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.theokanning.openai.OpenAiError;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.assistants.run.ToolCall;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import java.net.SocketTimeoutException;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import retrofit2.HttpException;
import retrofit2.Response;

class AiAdvisorServiceAutoCompleteTest {

  private AssistantService completionService;
  @Mock private OpenAiApi openAiApi;
  MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  OpenAIAssistantMocker openAIAssistantMocker;
  OpenAIAssistantThreadMocker openAIAssistantThreadMocker;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    completionService = new AiAdvisorService(openAiApi).getChatService("example-id");
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    openAIAssistantThreadMocker =
        openAIAssistantMocker.mockThreadCreation(null).mockCreateMessage();
  }

  @Nested
  class SimpleAutoComplete {

    @Test
    void aiUnderstandHowToCompete() {
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(
              new NoteDetailsCompletion(" must come down"),
              AiToolName.COMPLETE_NOTE_DETAILS.getValue())
          .mockRetrieveRun();
      assertEquals(" must come down", getAiCompletionAndResult("what goes up"));
    }

    @Test
    void aiTryToChatWithoutCallingAnyTool() {
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatCompleted()
          .mockRetrieveRun()
          .mockListMessages("Interesting idea.");
      assertEquals(
          "Interesting idea.",
          getAiCompletionResponse("what goes up")
              .getMessages()
              .getFirst()
              .getContent()
              .getFirst()
              .getText()
              .getValue());
    }

    @Test
    void getAiSuggestion_givenAString_whenHttpError_returnsEmptySuggestion()
        throws JsonProcessingException {
      HttpException httpException = BuildOpenAiException(400);
      Mockito.when(openAiApi.createRun(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Single.error(httpException));
      assertThrows(OpenAiHttpException.class, () -> getAiCompletionAndResult(""));
    }

    @Test
    void getAiSuggestion_when_timeout() {
      RuntimeException exception = new RuntimeException(new SocketTimeoutException());
      Mockito.when(openAiApi.createRun(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Single.error(exception));
      OpenAITimeoutException result =
          assertThrows(OpenAITimeoutException.class, () -> getAiCompletionAndResult(""));
      assertThat(result.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.OPENAI_TIMEOUT));
    }

    @Test
    void getAiSuggestion_when_got_502() throws JsonProcessingException {
      RuntimeException exception = BuildOpenAiException(502);
      Mockito.when(openAiApi.createRun(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Single.error(exception));
      OpenAIServiceErrorException result =
          assertThrows(OpenAIServiceErrorException.class, () -> getAiCompletionAndResult(""));
      assertThat(
          result.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.OPENAI_SERVICE_ERROR));
      assertThat(result.getMessage(), containsString("502"));
    }

    @Test
    void getAiSuggestion_given_invalidToken_return_401() throws JsonProcessingException {
      HttpException httpException = BuildOpenAiException(401);
      Mockito.when(openAiApi.createRun(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Single.error(httpException));
      OpenAiUnauthorizedException exception =
          assertThrows(OpenAiUnauthorizedException.class, () -> getAiCompletionAndResult(""));
      assertThat(exception.getMessage(), containsString("401"));
    }

    private String getAiCompletionAndResult(String incompleteContent) {
      ToolCall first = getAiCompletionResponse(incompleteContent).getToolCalls().getFirst();
      JsonNode arguments = first.getFunction().getArguments();

      return arguments.get("completion").asText();
    }

    private AiAssistantResponse getAiCompletionResponse(String incompleteContent) {
      Note note = makeMe.aNote().inMemoryPlease();
      return completionService.createThreadAndRunWithFirstMessage(note, "");
    }
  }

  private static HttpException BuildOpenAiException(int statusCode) throws JsonProcessingException {
    OpenAiError error = new OpenAiError(new OpenAiError.OpenAiErrorDetails());
    error.error.setMessage("%d".formatted(statusCode));
    return new HttpException(
        Response.error(
            statusCode,
            ResponseBody.create(
                defaultObjectMapper().writeValueAsString(error),
                MediaType.parse("application/json"))));
  }
}
