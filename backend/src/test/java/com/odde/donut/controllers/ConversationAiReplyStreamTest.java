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
  void streamsTextChunksThenDone() throws Exception {
    new OpenAiResponseStreamMocker(officialClient).withMessage("Hi").mockStreamResponse();

    requestAiReply()
        .andExpect(
            content()
                .string(
                    chunk("{\"choices\":[{\"delta\":{\"content\":\"Hi\"},\"finish_reason\":null}]}")
                        + chunk("{\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}")
                        + "event:done\ndata: [DONE]\n\n"));
  }

  @Test
  void streamsToolCallChunksThenDone() throws Exception {
    new OpenAiResponseStreamMocker(officialClient)
        .withToolCall("NoteContentCompletion", "{\"content\":", "\"X\"}")
        .mockStreamResponse();

    requestAiReply()
        .andExpect(
            content()
                .string(
                    chunk("{\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}")
                        + chunk(
                            "{\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":1,\"type\":\"function\","
                                + "\"function\":{\"name\":\"NoteContentCompletion\",\"arguments\":\"{\\\"content\\\":\"},"
                                + "\"id\":\"call-mock\"}]},\"finish_reason\":null}]}")
                        + chunk(
                            "{\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":1,\"type\":\"function\","
                                + "\"function\":{\"arguments\":\"\\\"X\\\"}\"}}]},\"finish_reason\":null}]}")
                        + chunk("{\"choices\":[{\"delta\":{},\"finish_reason\":\"tool_calls\"}]}")
                        + "event:done\ndata: [DONE]\n\n"));
  }

  private static String chunk(String json) {
    return "event:chat.completion.chunk\ndata: " + json + "\n\n";
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
