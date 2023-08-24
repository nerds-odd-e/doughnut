package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ChatServiceTest {

  @InjectMocks private ChatService target = new ChatService();
  @Mock private OpenAiApi openAiApi;

  MakeMeWithoutDB makeMe = new MakeMeWithoutDB();

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void getAnwserFromOpenApi() {
    // Arrange
    String expected = "I'm ChatGPT";
    Single<ChatCompletionResult> completionResultSingle =
        Single.just(makeMe.openAiCompletionResult().choice(expected).please());
    Mockito.when(openAiApi.createChatCompletion(Mockito.any())).thenReturn(completionResultSingle);

    // Act
    String askStatement = "What's your name?";
    String actual = target.askChatGPT(askStatement);

    // Assert
    assertEquals(expected, actual);
  }
}
