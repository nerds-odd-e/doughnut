package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

class ConversationMessageControllerTest extends ConversationMessageControllerTestBase {
  @Autowired EntityManager entityManager;

  @Nested
  class DeclineCorrection {
    @Test
    void retainsDeclineWithItsParticipantAndTimeWithoutChangingAuthoredContent() throws Exception {
      Note note =
          makeMe.aNote().notebookOwnedBy(currentUser.getUser()).content("Original").please();
      Conversation conversation =
          makeMe.aConversation().forANote(note).from(currentUser.getUser()).please();
      ConversationNoteCorrection correction =
          makeMe
              .aConversationNoteCorrection(makeMe.aConversationMessage(conversation).please())
              .please();
      Timestamp decidedAt =
          testabilitySettings.timeTravelTo(Timestamp.valueOf("2026-10-14 01:00:00"));

      controller.declineCorrection(correction.getId());
      entityManager.flush();
      entityManager.clear();
      Conversation fresh = conversationRepository.findById(conversation.getId()).orElseThrow();
      ConversationNoteCorrection stored = controller.getConversationCorrections(fresh).getFirst();

      assertThat(stored.getId(), equalTo(correction.getId()));
      assertThat(stored.getDecision(), equalTo(ConversationNoteCorrection.Decision.DECLINED));
      assertThat(stored.getDecidedBy().getId(), equalTo(currentUser.getUser().getId()));
      assertThat(stored.getDecidedAt(), equalTo(decidedAt));
      assertThat(stored.getProposedValue(), equalTo(correction.getProposedValue()));
      assertThat(stored.getReason(), equalTo(correction.getReason()));
      assertThat(stored.getSourceMessageId(), equalTo(correction.getSourceMessageId()));
      assertThat(stored.getNote().getContent(), equalTo("Original"));
    }

    @Test
    void aParticipantWithoutNoteEditRightsCanDeclineAndRetryKeepsTheirDecision() throws Exception {
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
      ConversationNoteCorrection correction =
          makeMe
              .aConversationNoteCorrection(makeMe.aConversationMessage(conversation).please())
              .please();
      var participant = currentUser.getUser();
      Timestamp decidedAt =
          testabilitySettings.timeTravelTo(Timestamp.valueOf("2026-10-14 01:00:00"));
      controller.declineCorrection(correction.getId());
      entityManager.flush();
      entityManager.clear();
      currentUser.setUser(conversation.getAccessOwnership().getUser());
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2026-10-14 02:00:00"));

      ConversationNoteCorrection repeated = controller.declineCorrection(correction.getId());

      assertThat(repeated.getDecidedBy().getId(), equalTo(participant.getId()));
      assertThat(repeated.getDecidedAt(), equalTo(decidedAt));
    }

    @Test
    void aCompletedAcceptanceReturnsItsRetainedDisposition() throws Exception {
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
      ConversationNoteCorrection correction =
          makeMe
              .aConversationNoteCorrection(makeMe.aConversationMessage(conversation).please())
              .acceptedBy(currentUser.getUser())
              .please();
      entityManager.flush();
      entityManager.clear();

      ConversationNoteCorrection repeated = controller.declineCorrection(correction.getId());

      assertThat(repeated.getDecision(), equalTo(ConversationNoteCorrection.Decision.ACCEPTED));
      assertThat(repeated.getAcceptedRevision(), equalTo("a".repeat(40)));
    }

    @Test
    void deniesSomeoneOutsideTheConversation() {
      ConversationNoteCorrection correction =
          makeMe
              .aConversationNoteCorrection(
                  makeMe.aConversationMessage(makeMe.aConversation().please()).please())
              .please();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.declineCorrection(correction.getId()));
    }

    @Test
    void requiresLogin() {
      ConversationNoteCorrection correction =
          makeMe
              .aConversationNoteCorrection(
                  makeMe.aConversationMessage(makeMe.aConversation().please()).please())
              .please();
      currentUser.setUser(null);

      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.declineCorrection(correction.getId()));

      assertThat(exception.getStatusCode(), equalTo(HttpStatusCode.valueOf(401)));
    }
  }

  @Nested
  class MarkConversationAsRead {
    Conversation conversation;

    @BeforeEach
    void setup() {
      conversation = makeMe.aConversation().from(currentUser.getUser()).please();
    }

    @Test
    void deniedForUninvolvedConversation() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.markConversationAsRead(makeMe.aConversation().please()));
    }

    @Test
    void marksOthersMessagesAsReadAndReturnsEmpty() throws UnexpectedNoAccessRightException {
      ConversationMessage msg =
          makeMe.aConversationMessage(conversation).sender(makeMe.aUser().please()).please();

      List<ConversationMessage> messages = controller.markConversationAsRead(conversation);

      assertThat(messages, hasSize(0));
      assertThat(msg.getReadByReceiver(), equalTo(true));
    }

    @Test
    void ownMessagesStayUnread() throws UnexpectedNoAccessRightException {
      ConversationMessage msg =
          makeMe.aConversationMessage(conversation).sender(currentUser.getUser()).please();

      controller.markConversationAsRead(conversation);

      assertThat(msg.getReadByReceiver(), equalTo(false));
    }
  }

  @Nested
  class ReplyToConversation {
    @Test
    void deniedWhenNotAParticipant() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.replyToConversation("hi", makeMe.aConversation().please()));
    }

    @Test
    void ownerCanReply() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      Conversation conversation =
          makeMe.aConversation().from(makeMe.aUser().please()).forANote(note).please();

      ConversationMessage conversationMessage =
          controller.replyToConversation("This is a message", conversation);

      assertThat(conversationMessage.getMessage(), equalTo("This is a message"));
    }

    @Test
    void initiatorCanReply() throws UnexpectedNoAccessRightException {
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();

      assertThat(
          controller.replyToConversation("This is a message", conversation).getMessage(),
          equalTo("This is a message"));
    }
  }

  @Nested
  class GetConversationMessages {
    @Test
    void requiresLogin() {
      Conversation conversation = makeMe.aConversation().please();
      currentUser.setUser(null);

      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.getConversationMessages(conversation));
      assertThat(exception.getStatusCode(), equalTo(HttpStatusCode.valueOf(401)));
    }

    @Test
    void deniedWhenNotAParticipant() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getConversationMessages(makeMe.aConversation().please()));
    }

    @Test
    void returnsMessagesForParticipant() throws UnexpectedNoAccessRightException {
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
      makeMe.aConversationMessage(conversation).please();

      assertThat(controller.getConversationMessages(conversation), hasSize(1));
    }
  }

  @Nested
  class ExportConversation {
    @Test
    void deniedWhenNotAParticipant() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.exportConversation(makeMe.aConversation().please()));
    }
  }
}
