package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.OpenAiResponseStreamMocker;
import com.openai.client.OpenAIClient;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ConversationAiReplyControllerTest extends ConversationMessageControllerTestBase {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Nested
  class GetAiReply {
    Conversation conversation;

    @BeforeEach
    void setup() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      conversation = makeMe.aConversation().forANote(note).from(currentUser.getUser()).please();
      new OpenAiResponseStreamMocker(officialClient)
          .withMessage("I am a Chatbot")
          .mockStreamResponse();
    }

    @Test
    void persistsAiReplyFromStream() throws UnexpectedNoAccessRightException, BadRequestException {
      makeMe
          .aConversationMessage(conversation)
          .sender(currentUser.getUser())
          .message("Hello!")
          .please();

      SseEmitter res = controller.getAiReply(conversation);
      assertThat(res.getTimeout(), nullValue());

      makeMe.refresh(conversation);
      assertThat(conversation.getConversationMessages(), hasSize(2));
      ConversationMessage aiMessage = conversation.getConversationMessages().get(1);
      assertThat(aiMessage.getMessage(), equalTo("I am a Chatbot"));
      assertThat(aiMessage.getSender(), nullValue());
    }
  }
}
