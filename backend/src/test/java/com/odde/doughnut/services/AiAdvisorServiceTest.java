package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionResult;
import java.util.Collections;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import retrofit2.HttpException;
import retrofit2.Response;

class AiAdvisorServiceTest {

  private AiAdvisorService aiAdvisorService;
  @Mock private OpenAiService openAiServiceMock;
  MakeMeWithoutDB makeMe = new MakeMeWithoutDB();

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    aiAdvisorService = new AiAdvisorService(openAiServiceMock);
  }

  @Test
  void getAiSuggestion_givenAString_returnsAiSuggestionObject() {
    CompletionResult completionResult =
        makeMe.openAiCompletionResult().choice("suggestion_value").please();
    Mockito.when(openAiServiceMock.createCompletion(Mockito.any())).thenReturn(completionResult);
    assertEquals(
        "suggestion_value", aiAdvisorService.getAiSuggestion("suggestion_prompt").suggestion());
  }

  @Test
  void getAiSuggestion_givenAString_whenHttpError_returnsEmptySuggestion() {
    HttpException httpException = buildHttpException(400);
    Mockito.when(openAiServiceMock.createCompletion(ArgumentMatchers.any()))
        .thenThrow(httpException);
    assertThrows(HttpException.class, () -> aiAdvisorService.getAiSuggestion("suggestion_prompt"));
  }

  @Test
  void getAiEngagingStory_givenAlistOfStrings_returnsAStory() {
    CompletionResult completionResult =
        makeMe.openAiCompletionResult().choice("This is an engaging story").please();
    Mockito.when(openAiServiceMock.createCompletion(Mockito.any())).thenReturn(completionResult);
    assertEquals(
        "This is an engaging story",
        aiAdvisorService.getEngagingStory(Collections.singletonList("title")).engagingStory());
  }

  @Test
  void getAiSuggestion_given_invalidToken_return_401() {
    HttpException httpExceptionMock = Mockito.mock(HttpException.class);
    Mockito.when(httpExceptionMock.code()).thenReturn(401);
    String unauthorized = "Unauthorized";
    Mockito.when(httpExceptionMock.getMessage()).thenReturn(unauthorized);
    Mockito.when(openAiServiceMock.createCompletion(ArgumentMatchers.any()))
        .thenThrow(httpExceptionMock);
    OpenAiUnauthorizedException exception =
        assertThrows(OpenAiUnauthorizedException.class, () -> aiAdvisorService.getAiSuggestion(""));
    assertEquals(unauthorized, exception.getMessage());
  }

  private static HttpException buildHttpException(int statusCode) {
    HttpException httpException =
        new HttpException(Response.error(statusCode, ResponseBody.create(null, "")));
    return httpException;
  }
}
