package com.odde.doughnut.controllers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.ConversationMessageRepository;
import com.odde.doughnut.entities.repositories.ConversationRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.ChatMessageForFineTuning;
import com.odde.doughnut.testability.builders.RecallPromptBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

class ConversationMessageControllerTest extends ControllerTestBase {

  @Autowired ConversationMessageController controller;

  @Autowired ConversationRepository conversationRepository;
  @Autowired ConversationMessageRepository conversationMessageRepository;
  AssessmentQuestionInstance assessmentQuestionInstance;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    Notebook notebook = makeMe.aNotebook().please();
    AssessmentAttempt assessmentAttempt =
        makeMe.anAssessmentAttempt(notebook.getCreatorEntity()).withOneQuestion().please();
    assessmentQuestionInstance = assessmentAttempt.getAssessmentQuestionInstances().get(0);
  }

  @Test
  void teststartConversationAboutAssessmentQuestionReturnsOk() {
    String feedback = "This is a feedback";
    controller.startConversationAboutAssessmentQuestion(feedback, assessmentQuestionInstance);
    List<Conversation> conversations = (List<Conversation>) conversationRepository.findAll();
    assertEquals(1, conversations.size());
  }

  @Test
  void testGetMessageDetailWhenstartConversationAboutAssessmentQuestionReturnsOk() {
    String feedback = "This is a feedback";
    Conversation conversation =
        controller.startConversationAboutAssessmentQuestion(feedback, assessmentQuestionInstance);

    List<Conversation> conversations = (List<Conversation>) conversationRepository.findAll();

    makeMe.refresh(conversation);
    var conversationDetail = conversation.getConversationMessages();
    assertEquals(1, conversations.size());
    assertEquals(1, conversationDetail.size());
    assertEquals(feedback, conversationDetail.getFirst().getMessage());
  }

  @Test
  void testGetFeedbackThreadsSendFromTheUser() {
    makeMe
        .aConversation()
        .forAnAssessmentQuestionInstance(assessmentQuestionInstance)
        .from(currentUser.getUser())
        .please();
    makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    List<Conversation> conversations = controller.getConversationsOfCurrentUser();
    assertEquals(1, conversations.size());
  }

  @Test
  void testGetFeedbackThreadsAsReceiver() {
    makeMe
        .theNotebook(assessmentQuestionInstance.getAssessmentAttempt().getNotebook())
        .owner(currentUser.getUser())
        .please();
    makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    List<Conversation> conversations = controller.getConversationsOfCurrentUser();
    assertEquals(1, conversations.size());
  }

  @Test
  void testGetFeedbackThreadsAsAMemberOfACircle() {
    Circle myCircle = makeMe.aCircle().hasMember(currentUser.getUser()).please();
    makeMe
        .theNotebook(assessmentQuestionInstance.getAssessmentAttempt().getNotebook())
        .owner(myCircle)
        .please();
    makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    List<Conversation> conversations = controller.getConversationsOfCurrentUser();
    assertEquals(1, conversations.size());
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
      makeMe
          .theNotebook(assessmentQuestionInstance.getAssessmentAttempt().getNotebook())
          .owner(currentUser.getUser())
          .please();
      Conversation conversation =
          makeMe
              .aConversation()
              .forAnAssessmentQuestionInstance(assessmentQuestionInstance)
              .please();
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
      note = makeMe.aNote().creatorAndOwner(noteOwner).please();
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
    void shouldstartConversationAboutAssessmentQuestionToConversation() {
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
      Conversation conv3 = makeMe.aConversation().from(currentUser.getUser()).please();

      // Add messages with specific timestamps
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
      makeMe
          .aConversationMessage(conv3)
          .sender(otherUser)
          .createdAt(makeMe.aTimestamp().of(1, 3).please())
          .please();

      List<Conversation> orderedConversations = controller.getConversationsOfCurrentUser();

      assertIterableEquals(List.of(conv3, conv2, conv1), orderedConversations);
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

      List<Conversation> orderedConversations = controller.getConversationsOfCurrentUser();

      assertIterableEquals(List.of(conv3, conv2, conv1), orderedConversations);
    }
  }

  @Nested
  class GetConversationsAboutNoteTests {
    Note note;
    User otherUser;

    @BeforeEach
    void setup() {
      User noteOwner = makeMe.aUser().please();
      note = makeMe.aNote().creatorAndOwner(noteOwner).please();
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
      RecallPromptBuilder recallPromptBuilder = makeMe.aRecallPrompt();
      recallPrompt = recallPromptBuilder.approvedQuestionOf(note).please();
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
              .creatorAndOwner(noteOwner)
              .titleConstructor("There are 42 prefectures in Japan")
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

    private String formatExportResponse(java.util.Map<String, Object> request) {
      StringBuilder export = new StringBuilder();
      export.append("## Context\n\n");
      java.util.List<java.util.Map<String, Object>> messages =
          (java.util.List<java.util.Map<String, Object>>) request.get("messages");
      for (java.util.Map<String, Object> message : messages) {
        if ("system".equals(message.get("role"))) {
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

    private String extractContentString(Object contentObj) {
      return ChatMessageForFineTuning.extractContentString(contentObj);
    }

    @Test
    void shouldExportConversationWithRequest() throws UnexpectedNoAccessRightException {
      java.util.Map<String, Object> request = controller.exportConversation(conversation);
      assertThat(request).isNotNull();
      assertThat(((java.util.List<?>) request.get("messages")).size()).isGreaterThan(0);
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
      String export = formatExportResponse(request);

      assertThat(export).contains("**User**: Is Naba one of them?");
      assertThat(export).contains("**Assistant**: No. It is not.");
    }

    @Test
    void shouldExportConversationWithContext() throws UnexpectedNoAccessRightException {
      java.util.Map<String, Object> request = controller.exportConversation(conversation);
      String export = formatExportResponse(request);
      assertThat(export).contains("## Context");
      assertThat(export).contains("Focus Note and the notes related to it:");
      assertThat(export).contains("There are 42 prefectures in Japan");
      assertThat(export).contains("Make tool calls when user asks to update the note.");
    }

    @Test
    void shouldExportConversationWithHistory() throws UnexpectedNoAccessRightException {
      makeMe
          .aConversationMessage(conversation)
          .sender(currentUser.getUser())
          .message("Is Naba one of them?")
          .please();

      java.util.Map<String, Object> request = controller.exportConversation(conversation);
      String export = formatExportResponse(request);
      assertThat(export).contains("## Conversation History");
    }
  }
}
