package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.ConversationListItem;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

class ConversationListingControllerTest extends ConversationMessageControllerTestBase {

  @Nested
  class ConversationOrdering {
    @Test
    void orderedByLastMessageTime() {
      Conversation conv1 = makeMe.aConversation().from(currentUser.getUser()).please();
      Conversation conv2 = makeMe.aConversation().from(currentUser.getUser()).please();
      User otherUser = makeMe.aUser().please();

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

      assertThat(
          controller.getConversationsOfCurrentUser().stream()
              .map(ConversationListItem::id)
              .toList(),
          equalTo(List.of(conv2.getId(), conv1.getId())));
    }

    @Test
    void orderedByCreationTimeWhenNoMessages() {
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

      assertThat(
          controller.getConversationsOfCurrentUser().stream()
              .map(ConversationListItem::id)
              .toList(),
          equalTo(List.of(conv3.getId(), conv2.getId(), conv1.getId())));
    }
  }

  @Nested
  class GetConversationsAboutNote {
    Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    }

    @Test
    void returnsConversationsAboutNote() {
      Conversation conv1 =
          makeMe.aConversation().from(currentUser.getUser()).forANote(note).please();
      Conversation conv2 =
          makeMe.aConversation().from(currentUser.getUser()).forANote(note).please();
      makeMe.aConversation().from(currentUser.getUser()).please();

      assertThat(controller.getConversationsAboutNote(note), containsInAnyOrder(conv1, conv2));
    }

    @Test
    void excludesInaccessibleConversations() {
      Conversation accessible =
          makeMe.aConversation().from(currentUser.getUser()).forANote(note).please();
      makeMe.aConversation().from(makeMe.aUser().please()).forANote(note).please();

      assertThat(controller.getConversationsAboutNote(note), contains(accessible));
    }

    @Test
    void requiresLogin() {
      currentUser.setUser(null);
      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class, () -> controller.getConversationsAboutNote(note));
      assertThat(exception.getStatusCode(), equalTo(HttpStatusCode.valueOf(401)));
    }
  }
}
