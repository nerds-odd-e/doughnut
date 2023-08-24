package com.odde.doughnut.services.openAiApis;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
