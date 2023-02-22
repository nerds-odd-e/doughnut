package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.ApiError;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import io.reactivex.Single;
import java.net.SocketTimeoutException;
import java.util.Collections;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import retrofit2.HttpException;
import retrofit2.Response;

class AiAdvisorServiceTest {

  private AiAdvisorService aiAdvisorService;
  @Mock private OpenAiApi openAiApi;
  @Captor ArgumentCaptor<CompletionRequest> completionRequestArgumentCaptor;
  MakeMeWithoutDB makeMe = new MakeMeWithoutDB();

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    aiAdvisorService = new AiAdvisorService(openAiApi);
  }

  @Nested
  class GetSuggestion {
    Single<CompletionResult> completionResultSingle =
        Single.just(makeMe.openAiCompletionResult().choice("what goes up must come down").please());

    Single<CompletionResult> IncompleteCompletionResultSingle =
        Single.just(
            makeMe.openAiCompletionResult().choiceReachingLengthLimit("what goes up").please());

    @Test
    void getAiSuggestion_givenAString_returnsAiSuggestionObject() {
      Mockito.when(openAiApi.createCompletion(Mockito.any())).thenReturn(completionResultSingle);
      assertEquals(
          "what goes up must come down",
          aiAdvisorService.getAiSuggestion("suggestion_prompt").getSuggestion());
    }

    @Test
    void the_data_returned_is_incomplete() {
      when(openAiApi.createCompletion(any())).thenReturn(IncompleteCompletionResultSingle);
      AiSuggestion suggestion = aiAdvisorService.getAiSuggestion("what");
      assertEquals("length", suggestion.getFinishReason());
    }

    @Test
    void getAiSuggestion_givenAString_whenHttpError_returnsEmptySuggestion() {
      HttpException httpException = buildHttpException(400);
      Mockito.when(openAiApi.createCompletion(ArgumentMatchers.any()))
          .thenReturn(Single.error(httpException));
      assertThrows(
          HttpException.class, () -> aiAdvisorService.getAiSuggestion("suggestion_prompt"));
    }

    @Test
    void getAiSuggestion_when_timeout() {
      RuntimeException exception = new RuntimeException(new SocketTimeoutException());
      Mockito.when(openAiApi.createCompletion(ArgumentMatchers.any()))
          .thenReturn(Single.error(exception));
      OpenAITimeoutException result =
          assertThrows(
              OpenAITimeoutException.class,
              () -> aiAdvisorService.getAiSuggestion("suggestion_prompt"));
      assertThat(result.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.OPENAI_TIMEOUT));
    }

    @Test
    void getAiSuggestion_given_invalidToken_return_401() {
      HttpException httpException = buildHttpException(401);
      Mockito.when(openAiApi.createCompletion(ArgumentMatchers.any()))
          .thenReturn(Single.error(httpException));
      OpenAiUnauthorizedException exception =
          assertThrows(
              OpenAiUnauthorizedException.class, () -> aiAdvisorService.getAiSuggestion(""));
      assertThat(exception.getMessage(), containsString("401"));
    }
  }

  @Nested
  class GetEngagingStory {
    @Test
    void getAiEngagingStory_givenAlistOfStrings_returnsAStory() {
      CompletionResult completionResult =
          makeMe.openAiCompletionResult().choice("This is an engaging story").please();
      Mockito.when(openAiApi.createCompletion(Mockito.any()))
          .thenReturn(Single.just(completionResult));
      assertEquals(
          "This is an engaging story",
          aiAdvisorService.getEngagingStory(Collections.singletonList("title")).engagingStory());
    }
  }

  private static HttpException buildHttpException(int statusCode) {
    return new HttpException(
        Response.error(statusCode, ResponseBody.create("{}", MediaType.parse("application/json"))));
  }
}
