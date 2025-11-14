package com.odde.doughnut.services.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
  private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    objectMapper = new ObjectMapperConfig().objectMapper();
  }

  @Nested
  class BuildHistory {

    @Test
    void shouldIncludeNoteContextAsFirstSystemMessage() {
      // Given a conversation about a note
      Note note = makeMe.aNote().please();
      Conversation conversation = makeMe.aConversation().forANote(note).please();

      // When building history
      ConversationHistoryBuilder builder = new ConversationHistoryBuilder(objectMapper);
      List<ChatMessage> history = builder.buildHistory(conversation);

      // Then first message should be system message with note context
      assertFalse(history.isEmpty());
      ChatMessage firstMessage = history.get(0);
      assertInstanceOf(SystemMessage.class, firstMessage);
      SystemMessage systemMessage = (SystemMessage) firstMessage;
      assertTrue(systemMessage.getContent().contains(note.getTopicConstructor()));
    }

    @Test
    void shouldIncludeUserAndAssistantMessages() {
      // Given a conversation with messages
      Note note = makeMe.aNote().please();
      User user = makeMe.aUser().please();
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
      ConversationHistoryBuilder builder = new ConversationHistoryBuilder(objectMapper);
      List<ChatMessage> history = builder.buildHistory(conversation);

      // Then should have system messages (note context + conversation instructions) + 3
      // conversation messages
      assertEquals(5, history.size());
      assertInstanceOf(SystemMessage.class, history.get(0)); // Note context
      assertInstanceOf(SystemMessage.class, history.get(1)); // Conversation instructions
      assertInstanceOf(UserMessage.class, history.get(2));
      // AI messages will be AssistantMessage
      assertInstanceOf(UserMessage.class, history.get(4));
    }

    @Test
    void shouldHandleEmptyConversation() {
      // Given a conversation with no messages
      Note note = makeMe.aNote().please();
      Conversation conversation = makeMe.aConversation().forANote(note).please();

      // When building history
      ConversationHistoryBuilder builder = new ConversationHistoryBuilder(objectMapper);
      List<ChatMessage> history = builder.buildHistory(conversation);

      // Then should have system messages (note context + conversation instructions)
      assertEquals(2, history.size());
      assertInstanceOf(SystemMessage.class, history.get(0)); // Note context
      assertInstanceOf(SystemMessage.class, history.get(1)); // Conversation instructions
    }
  }
}
