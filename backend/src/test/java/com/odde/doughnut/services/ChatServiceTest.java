package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

  @InjectMocks private ChatService target = new ChatService();
  @Mock private OpenAiApiHandler openAiApiHandler;

  @Test
  void getAnwserFromOpenApi() {
    // Arrange
    String expected = "I'm ChatGPT";
    Mockito.when(openAiApiHandler.getOpenAiAnswer("What's your name?")).thenReturn("I'm ChatGPT");

    // Act
    String askStatement = "What's your name?";
    String actual = target.askChatGPT(askStatement);

    // Assert
    assertEquals(expected, actual);
  }
}
