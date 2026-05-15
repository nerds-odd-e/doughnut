package com.odde.doughnut.services.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
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
class ConversationAiRequestBuilderTest {

  @Autowired MakeMe makeMe;

  @Autowired
  com.odde.doughnut.services.focusContext.FocusContextRetrievalService focusContextRetrievalService;

  @Autowired
  com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  @Nested
  class BuildParams {

    @Test
    void shouldIncludeNoteContextAsFirstDeveloperMessage() {
      Note note = makeMe.aNote().please();
      Conversation conversation = makeMe.aConversation().forANote(note).please();

      ConversationAiRequestBuilder builder =
          new ConversationAiRequestBuilder(
              focusContextRetrievalService, focusContextMarkdownRenderer);
      ResponseCreateParams params = builder.buildResponseCreateParams(conversation, "gpt-4.1-mini");
      List<ResponseInputItem> items = params.input().flatMap(i -> i.response()).orElseThrow();

      assertFalse(items.isEmpty());
      ResponseInputItem firstMessage = items.getFirst();
      assertTrue(firstMessage.easyInputMessage().isPresent());
      EasyInputMessage easy = firstMessage.easyInputMessage().orElseThrow();
      assertEquals(EasyInputMessage.Role.DEVELOPER, easy.role());
      String body = easy.content().asTextInput();
      assertTrue(body.contains(note.getTitle()));
      assertTrue(body.contains("# Focus Context"));
    }

    @Test
    void shouldIncludeUserAndAssistantMessages() {
      User user = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(user).please();
      Conversation conversation = makeMe.aConversation().forANote(note).from(user).please();

      makeMe.aConversationMessage(conversation).sender(user).message("First question").please();
      makeMe.aConversationMessage(conversation).sender(null).message("AI response").please();
      makeMe.aConversationMessage(conversation).sender(user).message("Follow up question").please();

      ConversationAiRequestBuilder builder =
          new ConversationAiRequestBuilder(
              focusContextRetrievalService, focusContextMarkdownRenderer);
      ResponseCreateParams params = builder.buildResponseCreateParams(conversation, "gpt-4.1-mini");
      List<ResponseInputItem> items = params.input().flatMap(i -> i.response()).orElseThrow();

      assertEquals(5, items.size());
      assertEquals(
          EasyInputMessage.Role.DEVELOPER, items.get(0).easyInputMessage().orElseThrow().role());
      assertEquals(
          EasyInputMessage.Role.DEVELOPER, items.get(1).easyInputMessage().orElseThrow().role());
      assertEquals(
          EasyInputMessage.Role.USER, items.get(2).easyInputMessage().orElseThrow().role());
      assertEquals(
          EasyInputMessage.Role.ASSISTANT, items.get(3).easyInputMessage().orElseThrow().role());
      assertEquals(
          EasyInputMessage.Role.USER, items.get(4).easyInputMessage().orElseThrow().role());
    }

    @Test
    void shouldHandleEmptyConversation() {
      Note note = makeMe.aNote().please();
      Conversation conversation = makeMe.aConversation().forANote(note).please();

      ConversationAiRequestBuilder builder =
          new ConversationAiRequestBuilder(
              focusContextRetrievalService, focusContextMarkdownRenderer);
      ResponseCreateParams params = builder.buildResponseCreateParams(conversation, "gpt-4.1-mini");
      List<ResponseInputItem> items = params.input().flatMap(i -> i.response()).orElseThrow();

      assertEquals(2, items.size());
      assertEquals(
          EasyInputMessage.Role.DEVELOPER, items.get(0).easyInputMessage().orElseThrow().role());
      assertEquals(
          EasyInputMessage.Role.DEVELOPER, items.get(1).easyInputMessage().orElseThrow().role());
    }
  }
}
