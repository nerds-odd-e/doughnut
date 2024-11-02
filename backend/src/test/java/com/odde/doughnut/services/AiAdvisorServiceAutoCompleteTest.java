package com.odde.doughnut.services;

import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.services.ai.AssistantService;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.theokanning.openai.OpenAiError;
import com.theokanning.openai.OpenAiHttpException;
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
    completionService = new AiAdvisorService(openAiApi).getContentCompletionService("example-id");
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
              new NoteDetailsCompletion(" must come down"), COMPLETE_NOTE_DETAILS)
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
      return getAiCompletionResponse(incompleteContent).getRequiredAction().getContentToAppend();
    }

    private AiAssistantResponse getAiCompletionResponse(String incompleteContent) {
      Note note = makeMe.aNote().inMemoryPlease();
      AiCompletionParams aiCompletionParams = new AiCompletionParams();
      aiCompletionParams.setDetailsToComplete(incompleteContent);
      return completionService.createThreadAndRunWithFirstMessage(
          note, aiCompletionParams.getCompletionPrompt());
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
