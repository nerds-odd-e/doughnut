package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.odde.doughnut.controllers.dto.ChatRequest;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.AiAdvisorWithStorageService;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.client.OpenAiApi;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
public class RestConversationMessageControllerAiReplyTests {

  @Mock private OpenAiApi openAiApi;

  @Autowired MakeMe makeMe;
  RestConversationMessageController controller;
  UserModel currentUser;
  Note note;
  TestabilitySettings testabilitySettings = new TestabilitySettings();
  OpenAIAssistantMocker openAIAssistantMocker;
  AiAdvisorService aiAdvisorService;
  AiAdvisorWithStorageService aiAdvisorWithStorageService;
  private ConversationService conversationService;
  Conversation conversation;

  @BeforeEach
  void setUp() {
    aiAdvisorService = new AiAdvisorService(openAiApi);
    aiAdvisorWithStorageService =
        new AiAdvisorWithStorageService(aiAdvisorService, makeMe.modelFactoryService);
    conversationService = new ConversationService(makeMe.modelFactoryService);
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestConversationMessageController(
            currentUser, conversationService, aiAdvisorWithStorageService);
    note = makeMe.aNote().creatorAndOwner(currentUser).please();
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    conversation = makeMe.aConversation().forANote(note).from(currentUser).please();
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
      SseEmitter res = controller.getAiReply(conversation);
      assertThat(res.getTimeout()).isNull();
      List<ResponseBodyEmitter.DataWithMediaType> events =
          peekIntoEmitterWithExtremelyInappropriateIntimacy(res);
      assertThat(events.size()).isEqualTo(18);
    }

    @Test
    void itWillPersistTheThreadId() throws UnexpectedNoAccessRightException {
      long oldCount = makeMe.modelFactoryService.userAssistantThreadRepository.count();
      controller.getAiReply(conversation);
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
      controller.getAiReply(conversation);
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

      controller.getAiReply(conversation);
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

    @Disabled
    @Test
    void continueChat() throws UnexpectedNoAccessRightException {
      controller.getAiReply(conversation);
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
                    aiAdvisorWithStorageService,
                    makeMe.aUser().toModelPlease(),
                    testabilitySettings)
                .chat(note, new ChatRequest("What's your name?", null)));
  }
}
