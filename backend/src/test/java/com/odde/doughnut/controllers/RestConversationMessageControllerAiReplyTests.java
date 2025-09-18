package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.RecallPromptBuilder;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.client.OpenAiApi;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import org.apache.coyote.BadRequestException;
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
public class RestConversationMessageControllerAiReplyTests {

  @Mock private OpenAiApi openAiApi;

  @Autowired MakeMe makeMe;
  RestConversationMessageController controller;
  UserModel currentUser;
  Note note;
  TestabilitySettings testabilitySettings = new TestabilitySettings();
  OpenAIAssistantMocker openAIAssistantMocker;
  NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory;
  private ConversationService conversationService;
  Conversation conversation;
  Timestamp currentUTCTimestamp;

  @BeforeEach
  void setUp() {
    currentUTCTimestamp = testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(0, 0).please());
    currentUser = makeMe.aUser().toModelPlease();
    note =
        makeMe
            .aNote()
            .creatorAndOwner(currentUser)
            .updatedAt(TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, -1))
            .please();

    setupServices();
    conversation = makeMe.aConversation().forANote(note).from(currentUser).please();
  }

  private void setupServices() {
    GlobalSettingsService globalSettingsService =
        new GlobalSettingsService(makeMe.modelFactoryService);
    notebookAssistantForNoteServiceFactory =
        new NotebookAssistantForNoteServiceFactory(
            openAiApi, globalSettingsService, getTestObjectMapper());
    conversationService = new ConversationService(testabilitySettings, makeMe.modelFactoryService);
    controller =
        new RestConversationMessageController(
            currentUser, conversationService, notebookAssistantForNoteServiceFactory);
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
  }

  private com.fasterxml.jackson.databind.ObjectMapper getTestObjectMapper() {
    return new ObjectMapperConfig().objectMapper();
  }

  @Nested
  class NewChatTests {
    @BeforeEach
    void setUp() {
      openAIAssistantMocker
          .mockThreadCreation("my-thread")
          .mockCreateMessage()
          .andARunStream("my-run-id")
          .withMessageDeltas("I", " am", " a", " Chatbot")
          .withMessageCompleted("I am a Chatbot")
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
    void chatWithAIAndGetResponse() throws UnexpectedNoAccessRightException, BadRequestException {
      SseEmitter res = controller.getAiReply(conversation);
      assertThat(res.getTimeout()).isNull();
      List<ResponseBodyEmitter.DataWithMediaType> events =
          peekIntoEmitterWithExtremelyInappropriateIntimacy(res);
      assertThat(events.size()).isEqualTo(21);
    }

    @Test
    void chatWithUseTheChatAssistant()
        throws UnexpectedNoAccessRightException, BadRequestException {
      GlobalSettingsService globalSettingsService =
          new GlobalSettingsService(makeMe.modelFactoryService);
      globalSettingsService
          .defaultAssistantId()
          .setKeyValue(makeMe.aTimestamp().please(), "chat-assistant");
      controller.getAiReply(conversation);
      ArgumentCaptor<RunCreateRequest> captor = ArgumentCaptor.forClass(RunCreateRequest.class);
      verify(openAiApi).createRunStream(any(), captor.capture());
      assertThat(captor.getValue().getAssistantId()).isEqualTo("chat-assistant");
    }

    @Test
    void shouldUseNotebookSpecificAssistantWhenAvailable()
        throws UnexpectedNoAccessRightException, BadRequestException {
      NotebookAssistant notebookAssistant = new NotebookAssistant();
      notebookAssistant.setAssistantId("notebook-assistant");
      notebookAssistant.setNotebook(note.getNotebook());
      notebookAssistant.setCreator(currentUser.getEntity());
      notebookAssistant.setCreatedAt(currentUTCTimestamp);
      makeMe.modelFactoryService.save(notebookAssistant);
      makeMe.refresh(note.getNotebook());

      controller.getAiReply(conversation);

      ArgumentCaptor<RunCreateRequest> captor = ArgumentCaptor.forClass(RunCreateRequest.class);
      verify(openAiApi).createRunStream(any(), captor.capture());
      assertThat(captor.getValue().getAssistantId()).isEqualTo("notebook-assistant");
    }

    @Test
    void shouldAddMessageToConversationWhenMessageCompleted()
        throws UnexpectedNoAccessRightException, BadRequestException {
      int initialMessageCount = conversation.getConversationMessages().size();

      controller.getAiReply(conversation);

      // Verify a new message was added to the conversation
      assertThat(conversation.getConversationMessages().size()).isEqualTo(initialMessageCount + 1);

      // Verify the content of the added message
      List<ConversationMessage> messages = conversation.getConversationMessages();
      ConversationMessage lastMessage = messages.get(messages.size() - 1);
      assertThat(lastMessage.getSender()).isNull(); // AI message should have no user
    }

    @Test
    void itShouldPersistThreadIdInConversation()
        throws UnexpectedNoAccessRightException, BadRequestException {
      assertThat(conversation.getAiAssistantThreadId()).isNull();
      assertThat(conversation.getLastAiAssistantThreadSync()).isNull();

      controller.getAiReply(conversation);

      assertThat(conversation.getAiAssistantThreadId()).isEqualTo("my-thread");
      assertThat(conversation.getLastAiAssistantThreadSync())
          .isEqualTo(testabilitySettings.getCurrentUTCTimestamp());
    }

    @Test
    void shouldUpdateSyncTimestampWhenAIMessageIsAdded()
        throws UnexpectedNoAccessRightException, BadRequestException {
      conversation.setLastAiAssistantThreadSync(currentUTCTimestamp);
      conversation.setAiAssistantThreadId("my-thread");
      makeMe.modelFactoryService.save(conversation);
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 1).please());
      controller.getAiReply(conversation);

      makeMe.refresh(conversation);
      // Verify timestamp was updated to the new time when AI message was added
      assertThat(conversation.getLastAiAssistantThreadSync()).isNotEqualTo(currentUTCTimestamp);
    }

    @Test
    void shouldSyncUnsentMessagesWithOpenAI()
        throws UnexpectedNoAccessRightException, BadRequestException {
      // Setup initial sync time
      conversation.setLastAiAssistantThreadSync(currentUTCTimestamp);
      conversation.setAiAssistantThreadId("my-thread");

      // Add some messages after the sync
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 2).please());
      conversationService.addMessageToConversation(
          conversation, currentUser.getEntity(), "Hello AI!");
      conversationService.addMessageToConversation(
          conversation, currentUser.getEntity(), "How are you?");

      controller.getAiReply(conversation);

      // Verify the message sent to OpenAI
      ArgumentCaptor<MessageRequest> captor = ArgumentCaptor.forClass(MessageRequest.class);
      verify(openAiApi).createMessage(any(), captor.capture());

      String expectedMessage =
          String.format(
              "user `%s` says:%n-----------------\nHello AI!\n\n"
                  + "user `%s` says:%n-----------------\nHow are you?\n\n",
              currentUser.getEntity().getName(), currentUser.getEntity().getName());

      assertThat(captor.getValue().getContent()).isEqualTo(expectedMessage);
    }

    @Test
    void shouldSaySomethingWhenNoNewMessages()
        throws UnexpectedNoAccessRightException, BadRequestException {
      // Set sync time to current time so there are no unsent messages
      conversation.setLastAiAssistantThreadSync(currentUTCTimestamp);
      conversation.setAiAssistantThreadId("my-thread");

      controller.getAiReply(conversation);

      // Verify default message is sent
      verify(openAiApi, times(0)).createMessage(any(), any());
    }

    @Test
    void shouldSetConversationInstructionsForRun()
        throws UnexpectedNoAccessRightException, BadRequestException {
      controller.getAiReply(conversation);

      ArgumentCaptor<RunCreateRequest> captor = ArgumentCaptor.forClass(RunCreateRequest.class);
      verify(openAiApi).createRunStream(any(), captor.capture());

      assertThat(captor.getValue().getAdditionalInstructions())
          .contains(
              "User is seeking for having a conversation, so don't call functions to update the note unless user asks explicitly.");
    }

    @Test
    void shouldIncludeNotebookAiAssistantInstructionsInRun()
        throws UnexpectedNoAccessRightException, BadRequestException {
      // Setup notebook AI assistant with custom instructions
      NotebookAiAssistant notebookAiAssistant = new NotebookAiAssistant();
      notebookAiAssistant.setNotebook(note.getNotebook());
      notebookAiAssistant.setAdditionalInstructionsToAi("Always use Spanish.");
      notebookAiAssistant.setCreatedAt(currentUTCTimestamp);
      notebookAiAssistant.setUpdatedAt(currentUTCTimestamp);
      makeMe.modelFactoryService.save(notebookAiAssistant);
      makeMe.refresh(note.getNotebook());

      controller.getAiReply(conversation);

      ArgumentCaptor<RunCreateRequest> captor = ArgumentCaptor.forClass(RunCreateRequest.class);
      verify(openAiApi).createRunStream(any(), captor.capture());

      String instructions = captor.getValue().getAdditionalInstructions();
      assertThat(instructions)
          .contains(
              "User is seeking for having a conversation, so don't call functions to update the note unless user asks explicitly.")
          .contains("Always use Spanish.");
    }
  }

  @Nested
  class NoteUpdateSyncTests {
    @BeforeEach
    void setUp() {
      openAIAssistantMocker
          .aThread("existing-thread-id")
          .mockCreateMessage()
          .andARunStream("my-run-id")
          .withMessageDeltas("I'm", " Chatbot")
          .mockTheRunStream();

      conversation.setAiAssistantThreadId("existing-thread-id");
    }

    @Test
    void shouldSendNoteUpdateMessageWhenNoteIsUpdatedAfterLastSync()
        throws UnexpectedNoAccessRightException, BadRequestException {
      conversation.setLastAiAssistantThreadSync(currentUTCTimestamp);

      makeMe
          .theNote(note)
          .details("Updated content")
          .updatedAt(makeMe.aTimestamp().of(0, 1).please())
          .please();

      controller.getAiReply(conversation);

      // Verify the note update message was sent
      ArgumentCaptor<MessageRequest> captor = ArgumentCaptor.forClass(MessageRequest.class);
      verify(openAiApi, times(1)).createMessage(any(), captor.capture());

      List<MessageRequest> messages = captor.getAllValues();
      String expectedUpdateMessage =
          String.format("The note content has been update:%n%n%s", note.getNoteDescription());
      assertThat(messages.get(0).getContent()).isEqualTo(expectedUpdateMessage);
      assertThat(messages.get(0).getRole()).isEqualTo("assistant");
    }

    @Test
    void shouldNotSendNoteUpdateMessageWhenNoteIsNotUpdated()
        throws UnexpectedNoAccessRightException, BadRequestException {
      conversation.setLastAiAssistantThreadSync(testabilitySettings.getCurrentUTCTimestamp());

      controller.getAiReply(conversation);

      verify(openAiApi, times(0)).createMessage(any(), any());
    }

    @Test
    void shouldNotSendNoteUpdateMessageWhenLastSyncIsNull()
        throws UnexpectedNoAccessRightException, BadRequestException {
      conversation.setLastAiAssistantThreadSync(null);

      controller.getAiReply(conversation);

      verify(openAiApi, times(0)).createMessage(any(), any());
    }
  }

  @Nested
  class RecallPromptConversationTests {
    RecallPrompt recallPrompt;
    Note questionNote;

    @BeforeEach
    void setup() {
      questionNote = makeMe.aNote().creatorAndOwner(currentUser).please();
      RecallPromptBuilder recallPromptBuilder = makeMe.aRecallPrompt();
      recallPrompt = recallPromptBuilder.approvedQuestionOf(questionNote).please();
      conversation =
          makeMe.aConversation().forARecallPrompt(recallPrompt).from(currentUser).please();

      openAIAssistantMocker
          .mockThreadCreation("my-thread")
          .mockCreateMessage()
          .andARunStream("my-run-id")
          .withMessageDeltas("I", " am", " a", " Chatbot")
          .withMessageCompleted("I am a Chatbot")
          .mockTheRunStream();
    }

    @Test
    void shouldUseNoteFromRecallPrompt()
        throws UnexpectedNoAccessRightException, BadRequestException {
      controller.getAiReply(conversation);

      // Verify the AI assistant is created with the correct note
      verify(openAiApi).createRunStream(any(), any());
    }

    @Test
    void shouldUpdateSyncTimestampWhenAIMessageIsAdded()
        throws UnexpectedNoAccessRightException, BadRequestException {
      conversation.setLastAiAssistantThreadSync(currentUTCTimestamp);
      conversation.setAiAssistantThreadId("my-thread");
      makeMe.modelFactoryService.save(conversation);
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 1).please());

      controller.getAiReply(conversation);

      makeMe.refresh(conversation);
      assertThat(conversation.getLastAiAssistantThreadSync()).isNotEqualTo(currentUTCTimestamp);
    }

    @Test
    void shouldIncludeQuestionDetailsWhenCreatingNewThread()
        throws UnexpectedNoAccessRightException, BadRequestException {
      // Capture the message sent to OpenAI
      ArgumentCaptor<ThreadRequest> threadRequestArgumentCaptor =
          ArgumentCaptor.forClass(ThreadRequest.class);

      controller.getAiReply(conversation);

      verify(openAiApi).createThread(threadRequestArgumentCaptor.capture());

      // Verify the question details were included in the message
      String expectedQuestionDetails = recallPrompt.getQuestionDetails();
      MessageRequest second = threadRequestArgumentCaptor.getValue().getMessages().get(1);
      assertThat(second.getContent().toString())
          .contains("User attempted to answer")
          .contains(expectedQuestionDetails);
      assertThat(second.getRole()).isEqualTo("assistant");
    }

    @Test
    void shouldNotIncludeQuestionDetailsWhenThreadExists()
        throws UnexpectedNoAccessRightException, BadRequestException {
      // Set existing thread ID
      conversation.setAiAssistantThreadId("existing-thread-id");
      makeMe.modelFactoryService.save(conversation);

      controller.getAiReply(conversation);

      // Verify no question details message was sent
      verify(openAiApi, times(0)).createMessage(any(), any());
    }
  }
}
