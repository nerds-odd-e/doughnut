package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionResult;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import retrofit2.HttpException;

class AiAdvisorServiceTest {

  private AiAdvisorService aiAdvisorService;

  @Mock private OpenAiService openAiServiceMock;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    aiAdvisorService = new AiAdvisorService(openAiServiceMock);
  }

  @Test
  void getAiSuggestion_givenAString_returnsAiSuggestionObject() {
    CompletionResult completionResult = new CompletionResult();
    CompletionChoice completionChoice = new CompletionChoice();
    completionChoice.setText("suggestion_value");
    List<CompletionChoice> completionChoices = List.of(completionChoice);
    completionResult.setChoices(completionChoices);
    AiSuggestion expected = new AiSuggestion("suggestion_value");

    Mockito.when(openAiServiceMock.createCompletion(Mockito.any())).thenReturn(completionResult);

    assertEquals(expected, aiAdvisorService.getAiSuggestion("suggestion_prompt"));
  }

  @Test
  void getAiSuggestion_givenAString_sendsOpenAIRequestWithRightParams() {
    CompletionResult completionResult = new CompletionResult();
    CompletionChoice completionChoice = new CompletionChoice();
    completionChoice.setText("suggestion_value");
    List<CompletionChoice> completionChoices = List.of(completionChoice);
    completionResult.setChoices(completionChoices);
    AiSuggestion expected = new AiSuggestion("suggestion_value");

    Mockito.when(openAiServiceMock.createCompletion(Mockito.any())).thenReturn(completionResult);

    assertEquals(expected, aiAdvisorService.getAiSuggestion("suggestion_prompt"));
  }

  @Test
  void getAiSuggestion_givenAString_whenHttpError_returnsEmptySuggestion() {
    AiSuggestion expected = new AiSuggestion("");
    HttpException httpExceptionMock = Mockito.mock(HttpException.class);
    Mockito.when(httpExceptionMock.code()).thenReturn(400);
    Mockito.when(openAiServiceMock.createCompletion(ArgumentMatchers.any()))
        .thenThrow(httpExceptionMock);

    assertEquals(expected, aiAdvisorService.getAiSuggestion("suggestion_prompt"));

    Mockito.verify(openAiServiceMock).createCompletion(ArgumentMatchers.any());
  }

  @Test
  void getAiEngagingStory_givenAlistOfStrings_returnsAStory() {

    AiEngagingStory expected = new AiEngagingStory("This is an engaging story");

    CompletionResult completionResult = new CompletionResult();
    CompletionChoice completionChoice = new CompletionChoice();
    completionChoice.setText("This is an engaging story");

    List<CompletionChoice> completionChoices = List.of(completionChoice);
    completionResult.setChoices(completionChoices);
    Mockito.when(openAiServiceMock.createCompletion(Mockito.any())).thenReturn(completionResult);

    assertEquals(expected, aiAdvisorService.getEngagingStory(Collections.singletonList("title")));
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

  @Test
  void getAiEngagingStory_removesPromptFromResult() {
    AiEngagingStory expected = new AiEngagingStory("This is an engaging story");

    CompletionResult completionResult = new CompletionResult();
    CompletionChoice completionChoice = new CompletionChoice();
    completionChoice.setText(
        "Tell me an engaging story to learn about title.\n\nThis is an engaging story");

    List<CompletionChoice> completionChoices = List.of(completionChoice);
    completionResult.setChoices(completionChoices);
    Mockito.when(openAiServiceMock.createCompletion(Mockito.any())).thenReturn(completionResult);

    assertEquals(expected, aiAdvisorService.getEngagingStory(Collections.singletonList("title")));
  }
}
