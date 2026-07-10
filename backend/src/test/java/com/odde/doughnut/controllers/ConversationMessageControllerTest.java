package com.odde.doughnut.controllers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.ConversationListItem;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.ConversationMessageRepository;
import com.odde.doughnut.entities.repositories.ConversationRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.testability.OpenAiResponseStreamMocker;
import com.openai.client.OpenAIClient;
import java.util.List;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ConversationMessageControllerTest extends ControllerTestBase {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired ConversationMessageController controller;

  @Autowired ConversationRepository conversationRepository;
  @Autowired ConversationMessageRepository conversationMessageRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class MarkConversationAsRead {
    Conversation conversation;

    @BeforeEach
    void setup() {
      conversation = makeMe.aConversation().from(currentUser.getUser()).please();
    }

    @Test
    void shouldNotBeAbleToMarkOtherPeopleMsg() {
      Conversation uninvolvedConversation = makeMe.aConversation().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.markConversationAsRead(uninvolvedConversation));
    }

    @Test
    void testMarkConversationAsReadReduceTheCount() throws UnexpectedNoAccessRightException {
      makeMe.aConversationMessage(conversation).sender(makeMe.aUser().please()).please();
      List<ConversationMessage> messages = controller.markConversationAsRead(conversation);
      assertThat(messages.size()).isEqualTo(0);
    }

    @Test
    void testMarkConversationAsReadByReceiver() throws UnexpectedNoAccessRightException {
      ConversationMessage msg =
          makeMe.aConversationMessage(conversation).sender(makeMe.aUser().please()).please();
      assertEquals(false, msg.getReadByReceiver());
      controller.markConversationAsRead(conversation);
      assertEquals(true, msg.getReadByReceiver());
    }

    @Test
    void testMarkConversationAsReadBySender() throws UnexpectedNoAccessRightException {
      ConversationMessage msg =
          makeMe.aConversationMessage(conversation).sender(currentUser.getUser()).please();
      controller.markConversationAsRead(conversation);
      assertEquals(false, msg.getReadByReceiver());
    }
  }

  @Nested
  class ReplyToConversation {
    @Test
    void shouldNotBeAbleToReplyToAConversationIAmNotIn() {
      Conversation conversation = makeMe.aConversation().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.replyToConversation("hi", conversation));
    }

    @Test
    void ownerShouldBeAbleToReply() throws UnexpectedNoAccessRightException {
      String message = "This is a message";
      User initiator = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      Conversation conversation = makeMe.aConversation().from(initiator).forANote(note).please();
      ConversationMessage conversationMessage =
          controller.replyToConversation(message, conversation);
      List<ConversationMessage> conversationMessages =
          (List<ConversationMessage>) conversationMessageRepository.findAll();
      assertEquals(1, conversationMessages.size());
      assertEquals(message, conversationMessage.getMessage());
    }

    @Test
    void initiatorShouldBeAbleToReply() throws UnexpectedNoAccessRightException {
      String message = "This is a message";
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
      ConversationMessage conversationMessage =
          controller.replyToConversation(message, conversation);
      assertEquals(message, conversationMessage.getMessage());
    }
  }

  @Nested
  class GetConversationDetails {

    @Test
    void shouldReturnUnauthorizedWhenNotLoggedIn() {
      Conversation conversation = makeMe.aConversation().please();
      currentUser.setUser(null);

      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.getConversationMessages(conversation));
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(401));
    }

    @Test
    void shouldNotBeAbleToSeeAConversationIAmNotIn() {
      Conversation conversation = makeMe.aConversation().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getConversationMessages(conversation));
    }

    @Test
    void testGetMessageThreadsFromConversation() throws UnexpectedNoAccessRightException {
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();

      makeMe.aConversationMessage(conversation).please();
      List<ConversationMessage> conversations = controller.getConversationMessages(conversation);
      assertEquals(1, conversations.size());
    }
  }

  @Nested
  class startConversationAboutNoteTests {
    Note note;
    String msg = "This is a feedback sent from note";

    @BeforeEach
    void setup() {
      User noteOwner = makeMe.aUser().please();
      note = makeMe.aNote().notebookOwnedBy(noteOwner).please();
    }

    @Test
    void shouldStartConversation() {
      controller.startConversationAboutNote(note, msg);
      List<Conversation> conversations = (List<Conversation>) conversationRepository.findAll();
      assertEquals(1, conversations.size());
      Conversation conversation = conversations.getFirst();
      assertEquals(conversation.getConversationInitiator(), currentUser.getUser());
    }

    @Test
    void shouldStartConversationAboutNoteAddsInitialMessage() {
      Conversation conversation = controller.startConversationAboutNote(note, msg);
      makeMe.refresh(conversation);
      List<ConversationMessage> conversationMessages = conversation.getConversationMessages();
      assertEquals(1, conversationMessages.size());
      ConversationMessage message = conversationMessages.getFirst();
      assertEquals(message.getMessage(), msg);
    }
  }

  @Nested
  class ConversationOrderingTests {
    User otherUser;

    @BeforeEach
    void setup() {
      otherUser = makeMe.aUser().please();
    }

    @Test
    void testConversationsOrderedByLastMessageTime() {
      Conversation conv1 = makeMe.aConversation().from(currentUser.getUser()).please();
      Conversation conv2 = makeMe.aConversation().from(currentUser.getUser()).please();

      makeMe
          .aConversationMessage(conv1)
          .sender(otherUser)
          .createdAt(makeMe.aTimestamp().of(1, 1).please())
          .please();
      makeMe
          .aConversationMessage(conv2)
          .sender(otherUser)
          .createdAt(makeMe.aTimestamp().of(1, 2).please())
          .please();

      List<ConversationListItem> orderedConversations = controller.getConversationsOfCurrentUser();

      assertIterableEquals(
          List.of(conv2.getId(), conv1.getId()),
          orderedConversations.stream().map(ConversationListItem::id).toList());
    }

    @Test
    void testConversationsOrderedByCreationTimeWhenNoMessages() {
      Conversation conv1 =
          makeMe
              .aConversation()
              .from(currentUser.getUser())
              .createdAt(makeMe.aTimestamp().of(1, 1).please())
              .please();
      Conversation conv2 =
          makeMe
              .aConversation()
              .from(currentUser.getUser())
              .createdAt(makeMe.aTimestamp().of(1, 2).please())
              .please();
      Conversation conv3 =
          makeMe
              .aConversation()
              .from(currentUser.getUser())
              .createdAt(makeMe.aTimestamp().of(1, 3).please())
              .please();

      List<ConversationListItem> orderedConversations = controller.getConversationsOfCurrentUser();

      assertIterableEquals(
          List.of(conv3.getId(), conv2.getId(), conv1.getId()),
          orderedConversations.stream().map(ConversationListItem::id).toList());
    }

    @Test
    void returnsAtMost50Conversations() {
      for (int i = 1; i <= 51; i++) {
        makeMe
            .aConversation()
            .from(currentUser.getUser())
            .createdAt(makeMe.aTimestamp().of(i, 0).please())
            .please();
      }
      assertEquals(50, controller.getConversationsOfCurrentUser().size());
    }
  }

  @Nested
  class GetConversationsAboutNoteTests {
    Note note;
    User otherUser;

    @BeforeEach
    void setup() {
      User noteOwner = makeMe.aUser().please();
      note = makeMe.aNote().notebookOwnedBy(noteOwner).please();
      otherUser = makeMe.aUser().please();
    }

    @Test
    void shouldReturnConversationsAboutNote() {
      // Create conversations about the note
      Conversation conv1 =
          makeMe.aConversation().from(currentUser.getUser()).forANote(note).please();
      Conversation conv2 =
          makeMe.aConversation().from(currentUser.getUser()).forANote(note).please();
      // Create an unrelated conversation
      makeMe.aConversation().from(currentUser.getUser()).please();

      List<Conversation> conversations = controller.getConversationsAboutNote(note);
      assertEquals(2, conversations.size());
      assertTrue(conversations.contains(conv1));
      assertTrue(conversations.contains(conv2));
    }

    @Test
    void shouldOnlyReturnAccessibleConversations() {
      // Create a conversation the current user initiated
      Conversation conv1 =
          makeMe.aConversation().from(currentUser.getUser()).forANote(note).please();
      // Create a conversation between other users about the same note
      Conversation conv2 = makeMe.aConversation().from(otherUser).forANote(note).please();

      List<Conversation> conversations = controller.getConversationsAboutNote(note);
      assertEquals(1, conversations.size());
      assertTrue(conversations.contains(conv1));
      assertFalse(conversations.contains(conv2));
    }

    @Test
    void shouldRequireLogin() {
      currentUser.setUser(null);
      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class, () -> controller.getConversationsAboutNote(note));
      assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
    }
  }

  @Nested
  class StartConversationAboutRecallPrompt {
    RecallPrompt recallPrompt;

    @BeforeEach
    void setup() {
      Note note = makeMe.aNote().please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();
      recallPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(memoryTracker)
              .withPredefinedQuestionForNote(note)
              .please();
    }

    @Test
    void shouldStartConversation() {
      Conversation conversation = controller.startConversationAboutRecallPrompt(recallPrompt);
      List<Conversation> conversations = (List<Conversation>) conversationRepository.findAll();
      assertEquals(1, conversations.size());
      assertEquals(conversation.getConversationInitiator(), currentUser.getUser());
    }

    @Test
    void shouldSetRecallPromptAsSubject() {
      Conversation conversation = controller.startConversationAboutRecallPrompt(recallPrompt);
      makeMe.refresh(conversation);
      assertEquals(recallPrompt, conversation.getSubject().getRecallPrompt());
      assertEquals(recallPrompt.getNotebook().getOwnership(), conversation.getSubjectOwnership());
    }
  }

  @Nested
  class ExportConversationTests {
    Note note;
    Conversation conversation;

    @BeforeEach
    void setup() {
      User noteOwner = makeMe.aUser().please();
      note =
          makeMe
              .aNote()
              .notebookOwnedBy(noteOwner)
              .title("There are 42 prefectures in Japan")
              .please();
      conversation = makeMe.aConversation().forANote(note).from(currentUser.getUser()).please();
    }

    @Test
    void shouldNotBeAbleToExportAConversationIAmNotIn() {
      Conversation otherConversation = makeMe.aConversation().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.exportConversation(otherConversation));
    }

    private List<java.util.Map<String, Object>> extractMessagesForExport(
        java.util.Map<String, Object> request) {
      Object legacy = request.get("messages");
      if (legacy instanceof List<?> list
          && !list.isEmpty()
          && list.get(0) instanceof java.util.Map) {
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> typed =
            (List<java.util.Map<String, Object>>) (List<?>) list;
        return typed;
      }
      Object inputObj = request.get("input");
      if (inputObj instanceof List<?> list) {
        return flattenResponseInputItems(list);
      }
      if (inputObj instanceof java.util.Map<?, ?> inputMap) {
        Object response = inputMap.get("response");
        if (response instanceof List<?> list) {
          return flattenResponseInputItems(list);
        }
      }
      return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<java.util.Map<String, Object>> flattenResponseInputItems(List<?> list) {
      List<java.util.Map<String, Object>> messages = new java.util.ArrayList<>();
      for (Object o : list) {
        if (!(o instanceof java.util.Map)) {
          continue;
        }
        java.util.Map<String, Object> item = (java.util.Map<String, Object>) o;
        Object role = item.get("role");
        if (!(role instanceof String)) {
          continue;
        }
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("role", role);
        row.put("content", contentToPlainText(item.get("content")));
        messages.add(row);
      }
      return messages;
    }

    private String contentToPlainText(Object content) {
      if (content instanceof String s) {
        return s;
      }
      if (content instanceof List<?> parts) {
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
          if (part instanceof java.util.Map<?, ?> p) {
            Object type = p.get("type");
            if ("input_text".equals(type) || "text".equals(type)) {
              Object text = p.get("text");
              if (text != null) {
                sb.append(text);
              }
            }
          }
        }
        return sb.toString();
      }
      return content != null ? content.toString() : "";
    }

    private String formatExportResponse(java.util.Map<String, Object> request) {
      StringBuilder export = new StringBuilder();
      export.append("## Context\n\n");
      java.util.List<java.util.Map<String, Object>> messages = extractMessagesForExport(request);
      for (java.util.Map<String, Object> message : messages) {
        if ("developer".equals(message.get("role"))) {
          export.append(formatMessage((String) message.get("content"))).append("\n\n");
        }
      }
      export.append("## Conversation History\n\n");
      for (java.util.Map<String, Object> message : messages) {
        if ("user".equals(message.get("role"))) {
          String content = (String) message.get("content");
          export.append("**User**: ").append(formatMessage(content)).append("\n");
        } else if ("assistant".equals(message.get("role"))) {
          String content = (String) message.get("content");
          export.append("**Assistant**: ").append(formatMessage(content)).append("\n");
        }
      }
      return export.toString();
    }

    private String formatMessage(String message) {
      return message.replaceAll("^\"|\"$", "").trim();
    }

    @Test
    void shouldExportConversationWithMessages() throws UnexpectedNoAccessRightException {
      makeMe
          .aConversationMessage(conversation)
          .sender(currentUser.getUser())
          .message("Is Naba one of them?")
          .please();
      makeMe.aConversationMessage(conversation).sender(null).message("No. It is not.").please();

      java.util.Map<String, Object> request = controller.exportConversation(conversation);
      assertThat(request).isNotNull();
      List<java.util.Map<String, Object>> messages = extractMessagesForExport(request);

      assertThat(messages.stream().filter(m -> "user".equals(m.get("role"))).findFirst())
          .get()
          .extracting(m -> m.get("content"))
          .isEqualTo("Is Naba one of them?");
      assertThat(messages.stream().filter(m -> "assistant".equals(m.get("role"))).findFirst())
          .get()
          .extracting(m -> m.get("content"))
          .isEqualTo("No. It is not.");
    }

    @Test
    void shouldExportConversationWithContextAndHistory() throws UnexpectedNoAccessRightException {
      makeMe
          .aConversationMessage(conversation)
          .sender(currentUser.getUser())
          .message("Is Naba one of them?")
          .please();

      java.util.Map<String, Object> request = controller.exportConversation(conversation);
      String export = formatExportResponse(request);
      assertThat(export).contains("## Context");
      assertThat(export).contains("# Focus Context");
      assertThat(export).contains("There are 42 prefectures in Japan");
      assertThat(export).contains("Make tool calls when user asks to update the note.");
      assertThat(export).contains("## Conversation History");
      assertThat(export).contains("Is Naba one of them?");
    }
  }

  @Nested
  class GetAiReplyTests {
    Note note;
    Conversation conversation;
    OpenAiResponseStreamMocker responseStreamMocker;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      conversation = makeMe.aConversation().forANote(note).from(currentUser.getUser()).please();
      responseStreamMocker = new OpenAiResponseStreamMocker(officialClient);
      responseStreamMocker.withMessage("I am a Chatbot").mockStreamResponse();
    }

    @Test
    void chatWithAIAndGetResponse() throws UnexpectedNoAccessRightException, BadRequestException {
      makeMe
          .aConversationMessage(conversation)
          .sender(currentUser.getUser())
          .message("Hello!")
          .please();

      SseEmitter res = controller.getAiReply(conversation);
      assertThat(res.getTimeout()).isNull();

      makeMe.refresh(conversation);
      assertEquals(2, conversation.getConversationMessages().size());
      ConversationMessage aiMessage = conversation.getConversationMessages().get(1);
      assertEquals("I am a Chatbot", aiMessage.getMessage());
      assertNull(aiMessage.getSender());
    }
  }
}
