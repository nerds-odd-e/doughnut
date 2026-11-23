package com.odde.donut.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.testability.OpenAiResponseStreamMocker;
import com.odde.donut.testability.SpringTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class ConversationAiReplyStreamTest extends SpringTestBase {
  @Autowired MockMvc mockMvc;
  Conversation conversation;

  @BeforeEach
  void setup() {
    User user = makeMe.aUser().please();
    currentUser.setUser(user);
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    conversation = makeMe.aConversation().forANote(note).from(user).please();
  }

  @Test
  void streamsChunksThenDone() throws Exception {
    new OpenAiResponseStreamMocker(officialClient).withMessage("Hi").mockStreamResponse();

    requestAiReply()
        .andExpect(content().string(containsString("event:chat.completion.chunk\ndata: ")))
        .andExpect(content().string(containsString("\"content\":\"Hi\"")))
        .andExpect(content().string(containsString("event:done\ndata: [DONE]")));
  }

  @Test
  void unauthorizedOpenAiEndsWithBadRequestEvent() throws Exception {
    new OpenAiResponseStreamMocker(officialClient)
        .failingWith(new RuntimeException("401 Unauthorized"))
        .mockStreamResponse();

    requestAiReply().andExpect(content().string(containsString("event:error\ndata: Bad Request")));
  }

  private ResultActions requestAiReply() throws Exception {
    return mockMvc.perform(post("/api/conversation/{id}/ai-reply", conversation.getId()));
  }
}
