package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.odde.doughnut.controllers.dto.ChatRequest;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationDetailService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.client.OpenAiApi;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RestAiControllerChatTests {

  @Mock private OpenAiApi openAiApi;
  @Autowired ConversationDetailService conversationDetailService;

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
            openAiApi,
            makeMe.modelFactoryService,
            conversationDetailService,
            currentUser,
            testabilitySettings);
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
          .andARunStream("my-run-id")
          .withMessageDeltas("I", " am", " a", " Chatbot")
          .mockTheRunStream();
    }

    @SuppressWarnings("unchecked")
    private List<ResponseBodyEmitter.DataWithMediaType>
        peekIntoEmitterWithExtremelyInappropriateIntimacy(SseEmitter sseEmitter) {
      try {
        Field field = ResponseBodyEmitter.class.getDeclaredField("earlySendAttempts");
        field.setAccessible(true);
        return ((Set<ResponseBodyEmitter.DataWithMediaType>) field.get(sseEmitter))
            .stream().toList();
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    @Test
    void chatWithAIAndGetResponse() throws UnexpectedNoAccessRightException {
      SseEmitter res = controller.chat(note, new ChatRequest("What's your name?", null));
      assertThat(res.getTimeout()).isNull();
      List<ResponseBodyEmitter.DataWithMediaType> events =
          peekIntoEmitterWithExtremelyInappropriateIntimacy(res);
      assertThat(events.size()).isEqualTo(18);
    }

    @Test
    void itWillPersistTheThreadId() throws UnexpectedNoAccessRightException {
      long oldCount = makeMe.modelFactoryService.userAssistantThreadRepository.count();
      controller.chat(note, new ChatRequest("What's your name?", null));
      long newCount = makeMe.modelFactoryService.userAssistantThreadRepository.count();
      assertThat(newCount).isEqualTo(oldCount + 1);
    }

    @Test
    void chatWithUseTheChatAssistant() throws UnexpectedNoAccessRightException {
      GlobalSettingsService globalSettingsService =
          new GlobalSettingsService(makeMe.modelFactoryService);
      globalSettingsService
          .noteCompletionAssistantId()
          .setKeyValue(makeMe.aTimestamp().please(), "chat-assistant");
      controller.chat(note, new ChatRequest("What's your name?", null));
      ArgumentCaptor<RunCreateRequest> captor = ArgumentCaptor.forClass(RunCreateRequest.class);
      verify(openAiApi).createRunStream(any(), captor.capture());
      assertThat(captor.getValue().getAssistantId()).isEqualTo("chat-assistant");
    }

    @Test
    void chatWithUseTheNotebookChatAssistantIfExisting() throws UnexpectedNoAccessRightException {
      NotebookAssistant notebookAssistant = new NotebookAssistant();
      notebookAssistant.setAssistantId("notebook-assistant");
      notebookAssistant.setNotebook(note.getNotebook());
      notebookAssistant.setCreator(currentUser.getEntity());
      notebookAssistant.setCreatedAt(makeMe.aTimestamp().please());
      makeMe.modelFactoryService.save(notebookAssistant);

      controller.chat(note, new ChatRequest("What's your name?", null));
      ArgumentCaptor<RunCreateRequest> captor = ArgumentCaptor.forClass(RunCreateRequest.class);
      verify(openAiApi).createRunStream(any(), captor.capture());
      assertThat(captor.getValue().getAssistantId()).isEqualTo("notebook-assistant");
    }
  }

  @Nested
  class ContinueChat {
    @BeforeEach
    void setUp() {
      openAIAssistantMocker
          .aThread("existing-thread-id")
          .mockCreateMessage()
          .andARunStream("my-run-id")
          .withMessageDeltas("I'm", " Chatbot")
          .mockTheRunStream();
    }

    @Test
    void continueChat() throws UnexpectedNoAccessRightException {
      controller.chat(note, new ChatRequest("What's your name?", "existing-thread-id"));
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
                    conversationDetailService,
                    makeMe.aUser().toModelPlease(),
                    testabilitySettings)
                .chat(note, new ChatRequest("What's your name?", null)));
  }

  @Nested
  class RestoreChat {
    @BeforeEach
    void setUp() {
      openAIAssistantMocker
          .aThread("my-existing-thread")
          .mockListMessages("previous message")
          .mockCreateMessage()
          .andARunStream("my-run-id")
          .withMessageDeltas("I'm", " Chatbot")
          .mockTheRunStream();
      makeMe.aUserAssistantThread("my-existing-thread").by(currentUser).forNote(note).please();
    }

    @Test
    void itWillLoadTheExistingThread() throws UnexpectedNoAccessRightException {
      List<Message> messages = controller.tryRestoreChat(note);
      assertThat(messages.size()).isEqualTo(1);
    }

    @Test
    void itMustGetTheMessageBackFirst() throws UnexpectedNoAccessRightException {
      controller.tryRestoreChat(note);
      verify(openAiApi, times(1))
          .listMessages(eq("my-existing-thread"), eq(Map.of("order", "asc")));
    }
  }
}
