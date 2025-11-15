package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.ChatCompletionConversationService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionStreamMocker;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.RecallPromptBuilder;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import java.sql.Timestamp;
import java.util.List;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ConversationMessageControllerAiReplyTests {

  @Mock private OpenAiApi openAiApi;

  @Autowired MakeMe makeMe;
  ConversationMessageController controller;
  UserModel currentUser;
  Note note;
  TestabilitySettings testabilitySettings = new TestabilitySettings();
  private ConversationService conversationService;
  Conversation conversation;

  @BeforeEach
  void setUp() {
    currentUser = makeMe.aUser().toModelPlease();
    note = makeMe.aNote().creatorAndOwner(currentUser).please();

    setupServices();
    conversation = makeMe.aConversation().forANote(note).from(currentUser).please();
  }

  private void setupServices() {
    GlobalSettingsService globalSettingsService =
        new GlobalSettingsService(makeMe.modelFactoryService);
    ObjectMapper objectMapper = getTestObjectMapper();
    OpenAiApiHandler openAiApiHandler = new OpenAiApiHandler(openAiApi);
    ChatCompletionConversationService chatCompletionConversationService =
        new ChatCompletionConversationService(
            openAiApiHandler, globalSettingsService, objectMapper);
    conversationService =
        new ConversationService(
            testabilitySettings, makeMe.modelFactoryService, chatCompletionConversationService);
    controller =
        new ConversationMessageController(
            currentUser, conversationService, chatCompletionConversationService);
  }

  private ObjectMapper getTestObjectMapper() {
    return new ObjectMapperConfig().objectMapper();
  }

  @Nested
  class NewChatTests {
    OpenAIChatCompletionStreamMocker chatMocker;

    @BeforeEach
    void setUp() {
      chatMocker = new OpenAIChatCompletionStreamMocker(openAiApi);
      chatMocker.withMessage("I am a Chatbot").mockStreamResponse();
    }

    @Test
    void chatWithAIAndGetResponse() throws UnexpectedNoAccessRightException, BadRequestException {
      // Add a user message first
      makeMe
          .aConversationMessage(conversation)
          .sender(currentUser.getEntity())
          .message("Hello!")
          .please();

      SseEmitter res = controller.getAiReply(conversation);
      assertThat(res.getTimeout()).isNull();

      // Verify AI message was saved to database
      makeMe.refresh(conversation);
      assertEquals(2, conversation.getConversationMessages().size());
      ConversationMessage aiMessage = conversation.getConversationMessages().get(1);
      assertEquals("I am a Chatbot", aiMessage.getMessage());
      assertNull(aiMessage.getSender()); // AI has no sender
    }

    @Test
    void shouldAddMessageToConversationWhenMessageCompleted()
        throws UnexpectedNoAccessRightException, BadRequestException {
      int initialMessageCount = conversation.getConversationMessages().size();

      controller.getAiReply(conversation);

      // Verify a new message was added to the conversation
      assertThat(conversation.getConversationMessages().size()).isEqualTo(initialMessageCount + 1);

      // Verify the content of the added message
      ConversationMessage lastMessage = conversation.getConversationMessages().getLast();
      assertThat(lastMessage.getSender()).isNull(); // AI message should have no user
    }

    @Test
    void shouldSetConversationInstructionsForRun()
        throws UnexpectedNoAccessRightException, BadRequestException {
      controller.getAiReply(conversation);

      ArgumentCaptor<ChatCompletionRequest> captor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi).createChatCompletionStream(captor.capture());

      // Verify conversation instructions are included in system messages
      List<ChatMessage> messages = captor.getValue().getMessages();
      boolean foundConversationInstructions =
          messages.stream()
              .filter(m -> m instanceof SystemMessage)
              .map(m -> ((SystemMessage) m).getTextContent())
              .anyMatch(
                  content ->
                      content.contains(
                          "User is seeking for having a conversation, so don't call functions to update the note unless user asks explicitly."));

      assertThat(foundConversationInstructions).isTrue();
    }

    @Test
    void shouldIncludeNotebookAiAssistantInstructionsInRun()
        throws UnexpectedNoAccessRightException, BadRequestException {
      // Setup notebook AI assistant with custom instructions
      Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
      NotebookAiAssistant notebookAiAssistant = new NotebookAiAssistant();
      notebookAiAssistant.setNotebook(note.getNotebook());
      notebookAiAssistant.setAdditionalInstructionsToAi("Always use Spanish.");
      notebookAiAssistant.setCreatedAt(currentUTCTimestamp);
      notebookAiAssistant.setUpdatedAt(currentUTCTimestamp);
      makeMe.modelFactoryService.save(notebookAiAssistant);
      makeMe.refresh(note.getNotebook());

      controller.getAiReply(conversation);

      ArgumentCaptor<ChatCompletionRequest> captor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi).createChatCompletionStream(captor.capture());

      // Verify notebook instructions are included in system messages
      List<ChatMessage> messages = captor.getValue().getMessages();
      boolean foundNotebookInstructions =
          messages.stream()
              .filter(m -> m instanceof SystemMessage)
              .map(m -> ((SystemMessage) m).getTextContent())
              .anyMatch(content -> content.contains("Always use Spanish."));

      assertThat(foundNotebookInstructions).isTrue();
    }
  }

  @Nested
  class RecallPromptConversationTests {
    RecallPrompt recallPrompt;
    Note questionNote;
    Conversation recallConversation;

    @BeforeEach
    void setup() {
      questionNote = makeMe.aNote().creatorAndOwner(currentUser).please();
      RecallPromptBuilder recallPromptBuilder = makeMe.aRecallPrompt();
      recallPrompt = recallPromptBuilder.approvedQuestionOf(questionNote).please();
      recallConversation =
          makeMe.aConversation().forARecallPrompt(recallPrompt).from(currentUser).please();

      OpenAIChatCompletionStreamMocker chatMocker = new OpenAIChatCompletionStreamMocker(openAiApi);
      chatMocker.withMessage("I am a Chatbot").mockStreamResponse();
    }

    @Test
    void shouldUseNoteFromRecallPrompt()
        throws UnexpectedNoAccessRightException, BadRequestException {
      controller.getAiReply(recallConversation);

      // Verify the chat completion request is made
      verify(openAiApi).createChatCompletionStream(any(ChatCompletionRequest.class));
    }

    @Test
    void shouldIncludeQuestionDetailsWhenCreatingNewConversation()
        throws UnexpectedNoAccessRightException, BadRequestException {
      controller.getAiReply(recallConversation);

      ArgumentCaptor<ChatCompletionRequest> captor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi).createChatCompletionStream(captor.capture());

      // Verify the question details were included in the system messages
      List<ChatMessage> messages = captor.getValue().getMessages();
      String expectedQuestionDetails = recallPrompt.getQuestionDetails();
      boolean foundQuestionDetails =
          messages.stream()
              .filter(m -> m instanceof SystemMessage)
              .map(m -> ((SystemMessage) m).getTextContent())
              .anyMatch(
                  content ->
                      content.contains("User attempted to answer")
                          && content.contains(expectedQuestionDetails));

      assertThat(foundQuestionDetails).isTrue();
    }
  }
}
