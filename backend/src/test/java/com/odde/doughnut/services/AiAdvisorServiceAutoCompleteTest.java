package com.odde.doughnut.services;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.json.AiCompletion;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.ApiError;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.services.ai.ClarifyingQuestion;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.OpenAiError;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunction;
import io.reactivex.Single;
import java.net.SocketTimeoutException;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
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
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    aiAdvisorService = new AiAdvisorService(openAiApi);
  }

  @Nested
  class GetSuggestion {
    @Test
    void getAiSuggestion_givenAString_returnsAiSuggestionObject() {
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCall(
          new NoteDetailsCompletion(" must come down"), "");
      assertEquals("what goes up must come down", getAiCompletionFromAdvisor("what goes up"));
    }

    @Test
    void getAiSuggestion_givenAString_whenHttpError_returnsEmptySuggestion()
        throws JsonProcessingException {
      HttpException httpException = BuildOpenAiException(400);
      Mockito.when(openAiApi.createChatCompletion(ArgumentMatchers.any()))
          .thenReturn(Single.error(httpException));
      assertThrows(OpenAiHttpException.class, () -> getAiCompletionFromAdvisor(""));
    }

    @Test
    void getAiSuggestion_when_timeout() {
      RuntimeException exception = new RuntimeException(new SocketTimeoutException());
      Mockito.when(openAiApi.createChatCompletion(ArgumentMatchers.any()))
          .thenReturn(Single.error(exception));
      OpenAITimeoutException result =
          assertThrows(OpenAITimeoutException.class, () -> getAiCompletionFromAdvisor(""));
      assertThat(result.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.OPENAI_TIMEOUT));
    }

    @Test
    void getAiSuggestion_when_got_502() throws JsonProcessingException {
      RuntimeException exception = BuildOpenAiException(502);
      Mockito.when(openAiApi.createChatCompletion(ArgumentMatchers.any()))
          .thenReturn(Single.error(exception));
      OpenAIServiceErrorException result =
          assertThrows(OpenAIServiceErrorException.class, () -> getAiCompletionFromAdvisor(""));
      assertThat(
          result.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.OPENAI_SERVICE_ERROR));
      assertThat(result.getMessage(), containsString("502"));
    }

    @Test
    void getAiSuggestion_given_invalidToken_return_401() throws JsonProcessingException {
      HttpException httpException = BuildOpenAiException(401);
      Mockito.when(openAiApi.createChatCompletion(ArgumentMatchers.any()))
          .thenReturn(Single.error(httpException));
      OpenAiUnauthorizedException exception =
          assertThrows(OpenAiUnauthorizedException.class, () -> getAiCompletionFromAdvisor(""));
      assertThat(exception.getMessage(), containsString("401"));
    }

    private String getAiCompletionFromAdvisor(String incompleteContent) {
      Note note = makeMe.aNote().inMemoryPlease();
      return aiAdvisorService
          .getAiCompletion(new AiCompletionParams(incompleteContent, null, null), note, "gpt-4")
          .getMoreCompleteContent();
    }
  }

  @Nested
  class CompleteNoteDetailWithClarifyingQuestion {
    Note note;
    AiCompletionParams params = new AiCompletionParams();
    ArgumentCaptor<ChatCompletionRequest> captor =
        ArgumentCaptor.forClass(ChatCompletionRequest.class);
    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().inMemoryPlease();
      openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    }

    @Test
    void askCompletionAndWithTwoFunctions() {
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCall(
          new ClarifyingQuestion("content not tested"), "");
      aiAdvisorService.getAiCompletion(params, note, "gpt-4");

      verify(openAiApi).createChatCompletion(captor.capture());
      assertEquals(2, captor.getValue().getFunctions().size());
      Assertions.assertThat(
              captor.getValue().getFunctions().stream()
                  .map(ChatFunction.class::cast)
                  .map(ChatFunction::getName))
          .contains("complete_note_details", "ask_clarification_question");
    }

    @Test
    void askCompletionAndUseQuestionResponse() {
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCall(
          new ClarifyingQuestion(
              "Are you referring to American football or association football (soccer) ?"),
          "ask_clarification_question");
      params.detailsToComplete = "Football ";
      AiCompletion aiCompletion = aiAdvisorService.getAiCompletion(params, note, "gpt-4");
      assertEquals("question", aiCompletion.getFinishReason());
      assertEquals(
          "Are you referring to American football or association football (soccer) ?",
          aiCompletion.getQuestion());
    }

    @Test
    void askCompletionAndUseStopResponseWithQuestionAnswer() {
      params.detailsToComplete = "Tea";
      params.questionFromAI = "Black tea or green tea?";
      params.answerFromUser = "green tea";
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCall(
          new NoteDetailsCompletion(" is common in China, if you are referring to green tea."),
          "complete_note_details");
      AiCompletion aiCompletion = aiAdvisorService.getAiCompletion(params, note, "gpt-4");
      assertEquals("stop", aiCompletion.getFinishReason());
      assertEquals(
          "Tea is common in China, if you are referring to green tea.",
          aiCompletion.getMoreCompleteContent());
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
