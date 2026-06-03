package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.testability.OpenAiResponseStreamMocker;
import com.openai.client.OpenAIClient;
import java.sql.Timestamp;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ConversationMessageControllerAiReplyTests extends ControllerTestBase {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired ConversationMessageController controller;
  Note note;
  Conversation conversation;

  @BeforeEach
  void setUp() {
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    conversation = makeMe.aConversation().forANote(note).from(currentUser.getUser()).please();
  }

  @Nested
  class NewChatTests {
    OpenAiResponseStreamMocker responseStreamMocker;

    @BeforeEach
    void setUp() {
      responseStreamMocker = new OpenAiResponseStreamMocker(officialClient);
      responseStreamMocker.withMessage("I am a Chatbot").mockStreamResponse();
    }

    @Test
    void chatWithAIAndGetResponse() throws UnexpectedNoAccessRightException, BadRequestException {
      // Add a user message first
      makeMe
          .aConversationMessage(conversation)
          .sender(currentUser.getUser())
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
    void shouldIncludeNotebookAiAssistantInstructionsInRun()
        throws UnexpectedNoAccessRightException, BadRequestException {
      // Setup notebook AI assistant with custom instructions
      Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
      NotebookAiAssistant notebookAiAssistant = new NotebookAiAssistant();
      notebookAiAssistant.setNotebook(note.getNotebook());
      notebookAiAssistant.setAdditionalInstructionsToAi("Always use Spanish.");
      notebookAiAssistant.setCreatedAt(currentUTCTimestamp);
      notebookAiAssistant.setUpdatedAt(currentUTCTimestamp);
      makeMe.entityPersister.save(notebookAiAssistant);
      makeMe.refresh(note.getNotebook());

      controller.getAiReply(conversation);

      assertThat(conversation.getConversationMessages().size()).isGreaterThan(0);
    }
  }
}
