package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.json.ChatRequest;
import com.odde.doughnut.controllers.json.ChatResponse;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RestAiControllerChatTests {

  @Mock private OpenAiApi openAiApi;

  @Autowired MakeMe makeMe;
  RestAiController controller;
  UserModel currentUser;
  Note note;
  Single<ChatCompletionResult> completionResultSingle;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setUp() {
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestAiController(
            openAiApi, makeMe.modelFactoryService, currentUser, testabilitySettings);
    note = makeMe.aNote().creatorAndOwner(currentUser).please();
    completionResultSingle =
        Single.just(makeMe.openAiCompletionResult().choice("I'm ChatGPT").please());
  }

  @Test
  void chatWithAIAndGetResponse() throws UnexpectedNoAccessRightException {
    Mockito.when(openAiApi.createChatCompletion(Mockito.any())).thenReturn(completionResultSingle);
    ChatResponse res = controller.chat(note, new ChatRequest("What's your name?"));
    assertEquals("I'm ChatGPT", res.getAssistantMessage());
  }

  @Test
  void chatWithNoteThatCannotAccess() {
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () ->
            new RestAiController(
                    openAiApi,
                    makeMe.modelFactoryService,
                    makeMe.aUser().toModelPlease(),
                    testabilitySettings)
                .chat(note, new ChatRequest("What's your name?")));
  }

  @Nested
  class ChatRequestTests {
    ArgumentCaptor<ChatCompletionRequest> argumentCaptor =
        ArgumentCaptor.forClass(ChatCompletionRequest.class);

    @BeforeEach
    void setUp() throws UnexpectedNoAccessRightException {
      Mockito.when(openAiApi.createChatCompletion(Mockito.any()))
          .thenReturn(completionResultSingle);
      controller.chat(note, new ChatRequest("What's your name?"));
      Mockito.verify(openAiApi).createChatCompletion(argumentCaptor.capture());
    }

    @Test
    void chatRequestShouldContainTheNoteDetails() {
      ChatMessage systemMessage = argumentCaptor.getValue().getMessages().get(1);
      assertThat(systemMessage.getRole()).isEqualTo("system");
      assertThat(systemMessage.getContent()).contains(note.getTopicConstructor());
    }

    @Test
    void chatRequestShouldContainTheUserQuestion() {
      ChatMessage userMessage = argumentCaptor.getValue().getMessages().get(2);
      assertThat(userMessage.getRole()).isEqualTo("user");
      assertThat(userMessage.getContent()).isEqualTo("What's your name?");
    }
  }
}
