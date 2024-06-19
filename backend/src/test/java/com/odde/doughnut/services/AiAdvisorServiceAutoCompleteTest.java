package com.odde.doughnut.services;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.services.ai.ClarifyingQuestion;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.theokanning.openai.OpenAiError;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.assistants.run.SubmitToolOutputRequestItem;
import com.theokanning.openai.assistants.run.SubmitToolOutputsRequest;
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

  private AiAdvisorService aiAdvisorService;
  @Mock private OpenAiApi openAiApi;
  MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  OpenAIAssistantMocker openAIAssistantMocker;
  OpenAIAssistantThreadMocker openAIAssistantThreadMocker;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    aiAdvisorService = new AiAdvisorService(openAiApi);
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    openAIAssistantThreadMocker =
        openAIAssistantMocker.mockThreadCreation(null).mockCreateMessage();
  }

  @Nested
  class SimpleAutoComplete {

    @Test
    void aiUnderstandHowToCompete() {
      openAIAssistantThreadMocker.mockCreateRunInProcess("my-run-id");
      openAIAssistantMocker.mockThreadRunCompletionToolCalled(
          new NoteDetailsCompletion(" must come down"), "my-run-id");
      assertEquals(" must come down", getAiCompletionAndResult("what goes up"));
    }

    @Test
    void aiTryToChatWithoutCallingAnyTool() {
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .mockRetrieveRunAndGetCompleted("my-run-id")
          .mockListMessages("Interesting idea.");
      assertEquals("Interesting idea.", getAiCompletionResponse("what goes up").getLastMessage());
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
      return aiAdvisorService.initiateAiCompletion(aiCompletionParams, note, "asst_example_id");
    }
  }

  @Nested
  class CompleteNoteDetailWithClarifyingQuestion {
    Note note;
    AiCompletionAnswerClarifyingQuestionParams params =
        new AiCompletionAnswerClarifyingQuestionParams();
    OpenAIAssistantMocker openAIAssistantMocker;

    @BeforeEach
    void setup() {
      params.setThreadId("any-thread-id");
      note = makeMe.aNote().inMemoryPlease();
      openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    }

    @Test
    void askCompletionAndUseQuestionResponse() {
      openAIAssistantMocker.mockSubmitOutputAndRequiredMoreAction(
          new ClarifyingQuestion(
              "Are you referring to American football or association football (soccer) ?"),
          "my-run-id");
      AiAssistantResponse aiAssistantResponse =
          aiAdvisorService.answerAiCompletionClarifyingQuestion(params);
      assertEquals("mocked-tool-call-id", aiAssistantResponse.getRequiredAction().toolCallId);
      assertEquals(
          "Are you referring to American football or association football (soccer) ?",
          aiAssistantResponse.getRequiredAction().getClarifyingQuestion().question);
    }

    @Nested
    class userAnswerToClarifyingQuestion {
      @BeforeEach
      void setup() {
        params.setDetailsToComplete("Tea");
        params.setAnswer("green tea");
      }

      @Test
      void mustSubmitTheAnswer() {
        openAIAssistantMocker.mockSubmitOutputAndCompletion(
            new NoteDetailsCompletion("blue planet"), "my-run-id");
        params.setToolCallId("tool-call-id");
        aiAdvisorService.answerAiCompletionClarifyingQuestion(params);
        ArgumentCaptor<SubmitToolOutputsRequest> captor =
            ArgumentCaptor.forClass(SubmitToolOutputsRequest.class);
        verify(openAiApi)
            .submitToolOutputs(ArgumentMatchers.any(), ArgumentMatchers.any(), captor.capture());
        SubmitToolOutputRequestItem submit = captor.getValue().getToolOutputs().get(0);
        assertThat(submit.getToolCallId(), equalTo("tool-call-id"));
        assertThat(submit.getOutput(), containsString("green tea"));
      }

      @Test
      void askCompletionAndUseStopResponseWithQuestionAnswer() {
        openAIAssistantMocker.mockSubmitOutputAndCompletion(
            new NoteDetailsCompletion(" is common in China, if you are referring to green tea."),
            "my-run-id");
        AiAssistantResponse aiAssistantResponse =
            aiAdvisorService.answerAiCompletionClarifyingQuestion(params);
        assertEquals(
            " is common in China, if you are referring to green tea.",
            aiAssistantResponse.getRequiredAction().getContentToAppend());
      }
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
