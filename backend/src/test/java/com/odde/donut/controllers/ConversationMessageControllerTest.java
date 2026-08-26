package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

class ConversationMessageControllerTest extends ConversationMessageControllerTestBase {

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
