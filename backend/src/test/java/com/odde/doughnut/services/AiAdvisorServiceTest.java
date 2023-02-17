package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import io.reactivex.Single;
import java.util.Collections;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Flux;
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
          aiAdvisorService.getAiSuggestion("suggestion_prompt").blockLast().suggestion());
    }

    @Test
    void the_data_returned_is_incomplete() {
      when(openAiApi.createCompletion(completionRequestArgumentCaptor.capture()))
          .thenReturn(IncompleteCompletionResultSingle);
      aiAdvisorService.getAiSuggestion("what").blockFirst();
      verify(openAiApi, Mockito.times(1)).createCompletion(any());
    }

    @Test
    void the_data_returned_is_incomplete_and_keep_receiving() {
      when(openAiApi.createCompletion(completionRequestArgumentCaptor.capture()))
          .thenReturn(IncompleteCompletionResultSingle)
          .thenReturn(completionResultSingle);
      AiSuggestion aiSuggestion = aiAdvisorService.getAiSuggestion("what").blockLast();
      assertEquals("what goes up must come down", aiSuggestion.suggestion());
      assertThat(
          completionRequestArgumentCaptor.getAllValues().get(1).getPrompt(),
          equalTo("what goes up"));
    }

    @Test
    void the_data_returned_is_incomplete_for_too_many_times() {
      when(openAiApi.createCompletion(any())).thenReturn(IncompleteCompletionResultSingle);
      aiAdvisorService.getAiSuggestion("what").blockLast();
      verify(openAiApi, Mockito.times(5)).createCompletion(any());
    }

    @Test
    void getAiSuggestion_givenAString_whenHttpError_returnsEmptySuggestion() {
      HttpException httpException = buildHttpException(400);
      Mockito.when(openAiApi.createCompletion(ArgumentMatchers.any()))
          .thenReturn(Single.error(httpException));
      Flux<AiSuggestion> suggestionFlux = aiAdvisorService.getAiSuggestion("suggestion_prompt");
      assertThrows(HttpException.class, () -> suggestionFlux.blockFirst());
    }

    @Test
    void getAiSuggestion_given_invalidToken_return_401() {
      HttpException httpException = buildHttpException(401);
      Mockito.when(openAiApi.createCompletion(ArgumentMatchers.any()))
          .thenReturn(Single.error(httpException));
      Flux<AiSuggestion> aiSuggestion = aiAdvisorService.getAiSuggestion("");
      OpenAiUnauthorizedException exception =
          assertThrows(OpenAiUnauthorizedException.class, () -> aiSuggestion.blockFirst());
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
