package com.odde.doughnut.services.openAiApis;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import retrofit2.HttpException;

@ExtendWith(MockitoExtension.class)
class OpenAiApiHandlerTest {
  @Mock private OpenAiApi openAiApi;
  @InjectMocks private OpenAiApiHandler target;
  MakeMeWithoutDB makeMe = new MakeMeWithoutDB();

  @Test
  void getAnwserFromOpenApi() {
    // Arrange
    String expected = "I'm ChatGPT";
    Single<ChatCompletionResult> completionResultSingle =
        Single.just(makeMe.openAiCompletionResult().choice(expected).please());
    Mockito.when(openAiApi.createChatCompletion(Mockito.any())).thenReturn(completionResultSingle);

    // Act
    OpenAiApiHandler target = new OpenAiApiHandler(openAiApi);
    String askStatement = "What's your name?";
    String actual = target.getOpenAiAnswer(askStatement);

    // Assert
    assertEquals(expected, actual);
  }

  @Test
  void getOpenApiAnwser_whenAnauthorized() {
    // Arrange
    OpenAiUnauthorizedException expected = new OpenAiUnauthorizedException("did not login");
    Mockito.when(openAiApi.createChatCompletion(Mockito.any()))
        .thenThrow(
            new HttpException(
                retrofit2.Response.error(
                    HttpStatus.UNAUTHORIZED.value(), ResponseBody.create(null, ""))));

    // Act
    OpenAiApiHandler target = new OpenAiApiHandler(openAiApi);
    String askStatement = "What's your name?";

    // Assert
    assertThrows(OpenAiUnauthorizedException.class, () -> target.getOpenAiAnswer(askStatement));
  }
}
