package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import retrofit2.HttpException;
import retrofit2.Response;

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
    CompletionRequest completionRequest =
        CompletionRequest.builder()
            .prompt("Tell me about suggestion_prompt.")
            .model("text-davinci-003")
            .maxTokens(100)
            .echo(true)
            .build();
    CompletionResult completionResult = new CompletionResult();
    CompletionChoice completionChoice = new CompletionChoice();
    completionChoice.setText("suggestion_value");
    List<CompletionChoice> completionChoices = List.of(completionChoice);
    completionResult.setChoices(completionChoices);
    AiSuggestion expected = new AiSuggestion("suggestion_value");

    Mockito.when(openAiServiceMock.createCompletion(completionRequest))
        .thenReturn(completionResult);

    assertEquals(expected, aiAdvisorService.getAiSuggestion("suggestion_prompt"));

    Mockito.verify(openAiServiceMock).createCompletion(completionRequest);
  }

  @Test
  void getAiSuggestion_givenAString_whenHttpError_returnsEmptySuggestion() {
    CompletionRequest completionRequest =
        CompletionRequest.builder()
            .prompt("Tell me about suggestion_prompt.")
            .model("text-davinci-003")
            .maxTokens(100)
            .echo(true)
            .build();

    AiSuggestion expected = new AiSuggestion("");
    HttpException exceptionFromOpenAi =
        new HttpException(
            Response.error(403, ResponseBody.create("response", MediaType.parse("plain/text"))));

    Mockito.when(openAiServiceMock.createCompletion(completionRequest))
        .thenThrow(exceptionFromOpenAi);

    assertEquals(expected, aiAdvisorService.getAiSuggestion("suggestion_prompt"));

    Mockito.verify(openAiServiceMock).createCompletion(completionRequest);
  }
}
