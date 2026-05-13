package com.odde.doughnut.services.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.chat.completions.ChatCompletionDeveloperMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConversationHistoryBuilderTest {

  @Autowired MakeMe makeMe;

  @Autowired
  com.odde.doughnut.services.focusContext.FocusContextRetrievalService focusContextRetrievalService;

  @Autowired
  com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  @Nested
  class BuildHistory {

    @Test
    void shouldIncludeNoteContextAsFirstSystemMessage() {
      // Given a conversation about a note
      Note note = makeMe.aNote().please();
      Conversation conversation =
          makeMe.aConversation().forANote(note).from(note.getCreator()).please();

      // When building history
      ConversationHistoryBuilder builder =
          new ConversationHistoryBuilder(
              focusContextRetrievalService, focusContextMarkdownRenderer);
      List<ChatCompletionMessageParam> history = builder.buildHistory(conversation);

      // Then first message should be developer message with note context
      assertFalse(history.isEmpty());
      ChatCompletionMessageParam firstMessage = history.get(0);
      assertTrue(firstMessage.developer().isPresent());
      ChatCompletionDeveloperMessageParam developerMessage = firstMessage.developer().get();
      String body = developerMessage.content().toString();
      assertTrue(body.contains(note.getTitle()));
      assertTrue(body.contains("# Focus Context"));
    }

    @Test
    void shouldIncludeUserAndAssistantMessages() {
      // Given a conversation with messages
      User user = makeMe.aUser().please();
      Note note = makeMe.aNote().nbCreatorAndOwner(user).please();
      Conversation conversation = makeMe.aConversation().forANote(note).from(user).please();

      ConversationMessage userMsg1 =
          makeMe.aConversationMessage(conversation).sender(user).message("First question").please();
      ConversationMessage aiMsg =
          makeMe.aConversationMessage(conversation).sender(null).message("AI response").please();
      ConversationMessage userMsg2 =
          makeMe
              .aConversationMessage(conversation)
              .sender(user)
              .message("Follow up question")
              .please();

      // When building history
      ConversationHistoryBuilder builder =
          new ConversationHistoryBuilder(
              focusContextRetrievalService, focusContextMarkdownRenderer);
      List<ChatCompletionMessageParam> history = builder.buildHistory(conversation);

      // Then should have system messages (note context + conversation instructions) + 3
      // conversation messages
      assertEquals(5, history.size());
      assertTrue(history.get(0).developer().isPresent()); // Note context
      assertTrue(history.get(1).developer().isPresent()); // Conversation instructions
      assertTrue(history.get(2).user().isPresent());
      // AI messages will be assistant
      assertTrue(history.get(4).user().isPresent());
    }

    @Test
    void shouldHandleEmptyConversation() {
      // Given a conversation with no messages
      Note note = makeMe.aNote().please();
      Conversation conversation =
          makeMe.aConversation().forANote(note).from(note.getCreator()).please();

      // When building history
      ConversationHistoryBuilder builder =
          new ConversationHistoryBuilder(
              focusContextRetrievalService, focusContextMarkdownRenderer);
      List<ChatCompletionMessageParam> history = builder.buildHistory(conversation);

      // Then should have system messages (note context + conversation instructions)
      assertEquals(2, history.size());
      assertTrue(history.get(0).developer().isPresent()); // Note context
      assertTrue(history.get(1).developer().isPresent()); // Conversation instructions
    }
  }
}
