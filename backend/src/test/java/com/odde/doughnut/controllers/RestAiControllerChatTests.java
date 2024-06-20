package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.odde.doughnut.controllers.dto.ChatRequest;
import com.odde.doughnut.controllers.dto.ChatResponse;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.*;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
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
  Single<ChatCompletionResult> completionResultSingle;
  TestabilitySettings testabilitySettings = new TestabilitySettings();
  OpenAIAssistantMocker openAIAssistantMocker;

  @BeforeEach
  void setUp() {
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestAiController(
            openAiApi, makeMe.modelFactoryService, currentUser, testabilitySettings);
    note = makeMe.aNote().creatorAndOwner(currentUser).please();
    completionResultSingle =
        Single.just(makeMe.openAiCompletionResult().choice("I'm ChatGPT").please());

    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
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
    ChatResponse res = controller.chat(note, new ChatRequest("What's your name?"));
    assertEquals("I'm Chatbot", res.getAssistantMessage());
  }

  @Test
  void chatWithUseTheChatAssistant() throws UnexpectedNoAccessRightException {
    GlobalSettingsService globalSettingsService =
        new GlobalSettingsService(makeMe.modelFactoryService);
    globalSettingsService
        .getNoteCompletionAssistantId()
        .setKeyValue(makeMe.aTimestamp().please(), "chat-assistant");
    controller.chat(note, new ChatRequest("What's your name?"));
    ArgumentCaptor<RunCreateRequest> captor = ArgumentCaptor.forClass(RunCreateRequest.class);
    verify(openAiApi).createRun(any(), captor.capture());
    assertThat(captor.getValue().getAssistantId()).isEqualTo("chat-assistant");
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
