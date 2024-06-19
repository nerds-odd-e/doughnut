package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.ChatRequest;
import com.odde.doughnut.controllers.dto.ChatResponse;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMock;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
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
  OpenAIAssistantMock openAIAssistantMock;
  OpenAIAssistantThreadMocker openAIAssistantThreadMocker;

  @BeforeEach
  void setUp() {
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestAiController(
            openAiApi, makeMe.modelFactoryService, currentUser, testabilitySettings);
    note = makeMe.aNote().creatorAndOwner(currentUser).please();
    completionResultSingle =
        Single.just(makeMe.openAiCompletionResult().choice("I'm ChatGPT").please());

    openAIAssistantMock = new OpenAIAssistantMock(openAiApi);
    openAIAssistantThreadMocker = openAIAssistantMock.mockThreadCreation(null);
    when(openAiApi.createMessage(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Single.just(new Message()));
    openAIAssistantMock.mockThreadRunCompletedAndListMessage("I'm Chatbot", "my-run-id");
  }

  @Test
  void chatWithAIAndGetResponse() throws UnexpectedNoAccessRightException {
    ChatResponse res = controller.chat(note, new ChatRequest("What's your name?"));
    assertEquals("I'm Chatbot", res.getAssistantMessage());
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
}
