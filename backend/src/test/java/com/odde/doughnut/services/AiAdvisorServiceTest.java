package com.odde.doughnut.services;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.json.AiCompletion;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.ApiError;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.OpenAiError;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import io.reactivex.Single;
import java.net.SocketTimeoutException;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import retrofit2.HttpException;
import retrofit2.Response;

class AiAdvisorServiceTest {

  private AiAdvisorService aiAdvisorService;
  @Mock private OpenAiApi openAiApi;
  MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    aiAdvisorService = new AiAdvisorService(openAiApi);
  }

  @Nested
  class GetSuggestion {
    Single<ChatCompletionResult> completionResultSingle =
        Single.just(makeMe.openAiCompletionResult().choice("must come down").please());

    Single<ChatCompletionResult> IncompleteCompletionResultSingle =
        Single.just(
            makeMe.openAiCompletionResult().choiceReachingLengthLimit("what goes up").please());

    @Test
    void getAiSuggestion_givenAString_returnsAiSuggestionObject() {
      Mockito.when(openAiApi.createChatCompletion(Mockito.any()))
          .thenReturn(completionResultSingle);
      assertEquals(
          "what goes up must come down",
          getAiCompletionFromAdvisor("what goes up").getMoreCompleteContent());
    }

    @Test
    void the_data_returned_is_incomplete() {
      when(openAiApi.createChatCompletion(any())).thenReturn(IncompleteCompletionResultSingle);
      AiCompletion suggestion = getAiCompletionFromAdvisor("");
      assertEquals("length", suggestion.getFinishReason());
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
  }

  private AiCompletion getAiCompletionFromAdvisor(String incompleteContent) {
    Note note = makeMe.aNote().inMemoryPlease();
    return aiAdvisorService.getAiCompletion(
        new AiCompletionParams(incompleteContent), note, "gpt-4");
  }

  @Nested
  class GetImage {
    @Test
    void getImageBasedOnPrompt() {
      ImageResult result = new ImageResult();
      Image image = new Image();
      image.setB64Json("https://image.com");
      result.setData(List.of(image));
      Mockito.when(openAiApi.createImage(Mockito.any())).thenReturn(Single.just(result));
      assertEquals("https://image.com", aiAdvisorService.getImage("prompt"));
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
