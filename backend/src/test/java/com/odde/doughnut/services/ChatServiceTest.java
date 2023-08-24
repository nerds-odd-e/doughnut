package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ChatServiceTest {

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
    List messages = new ArrayList<ChatMessage>();
    ChatMessage message1 = new ChatMessage(ChatMessageRole.USER.value(), "");
    ChatMessage message2 = new ChatMessage(ChatMessageRole.ASSISTANT.value(), "What's your name?");
    messages.add(message1);
    messages.add(message2);

    ChatCompletionRequest request =
        ChatCompletionRequest.builder().model("gpt-4").messages(messages).stream(false)
            .n(1)
            .maxTokens(100)
            .build();

    Optional<ChatCompletionChoice> result =
        openAiApi.createChatCompletion(request).blockingGet().getChoices().stream().findFirst();

    // Assert
    assertEquals(expected, result.get().getMessage().getContent());
  }
}
