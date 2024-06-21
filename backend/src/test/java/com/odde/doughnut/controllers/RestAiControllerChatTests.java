package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.controllers.dto.ChatRequest;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.*;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.client.OpenAiApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
  TestabilitySettings testabilitySettings = new TestabilitySettings();
  OpenAIAssistantMocker openAIAssistantMocker;

  @BeforeEach
  void setUp() {
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestAiController(
            openAiApi, makeMe.modelFactoryService, currentUser, testabilitySettings);
    note = makeMe.aNote().creatorAndOwner(currentUser).please();
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
  }

  @Nested
  class NewChat {
    @BeforeEach
    void setUp() {
      openAIAssistantMocker
          .mockThreadCreation("my-thread")
          .mockCreateMessage()
          .mockCreateRunInProcess("my-run-id")
          .aRunThatCompleted()
          .mockRetrieveRun()
          .mockListMessages("I'm Chatbot");
    }

    @Test
    void chatWithAIAndGetResponse() throws UnexpectedNoAccessRightException {
      AiAssistantResponse res =
          controller.chat(note, new ChatRequest("What's your name?", null, null));
      assertEquals(
          "I'm Chatbot", res.getMessages().getFirst().getContent().getFirst().getText().getValue());
    }

    @Test
    void chatWithUseTheChatAssistant() throws UnexpectedNoAccessRightException {
      GlobalSettingsService globalSettingsService =
          new GlobalSettingsService(makeMe.modelFactoryService);
      globalSettingsService
          .chatAssistantId()
          .setKeyValue(makeMe.aTimestamp().please(), "chat-assistant");
      controller.chat(note, new ChatRequest("What's your name?", null, null));
      ArgumentCaptor<RunCreateRequest> captor = ArgumentCaptor.forClass(RunCreateRequest.class);
      verify(openAiApi).createRun(any(), captor.capture());
      assertThat(captor.getValue().getAssistantId()).isEqualTo("chat-assistant");
    }
  }

  @Nested
  class ContinueChat {
    @BeforeEach
    void setUp() {
      openAIAssistantMocker
          .aThread("existing-thread-id")
          .mockCreateMessage()
          .mockCreateRunInProcess("my-run-id")
          .aRunThatCompleted()
          .mockRetrieveRun()
          .mockListMessages("I'm Chatbot");
    }

    @Test
    void continueChat() throws UnexpectedNoAccessRightException {
      controller.chat(
          note, new ChatRequest("What's your name?", "existing-thread-id", "last-msg-id"));
      ArgumentCaptor<MessageRequest> captor = ArgumentCaptor.forClass(MessageRequest.class);
      verify(openAiApi).createMessage(any(), captor.capture());
      assertThat(captor.getValue().getContent().toString()).isEqualTo("What's your name?");
    }
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
                .chat(note, new ChatRequest("What's your name?", null, null)));
  }
}
