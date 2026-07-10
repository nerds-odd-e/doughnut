package com.odde.doughnut.services.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.responses.EasyInputMessage;
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

      List<ResponseInputItem> items = inputItems(conversation);

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
    void firstDeveloperMessageShouldIncludeFocusNoteNotebookLabelAndContent() {
      Note note = makeMe.aNote().content("description").please();
      Conversation conversation = makeMe.aConversation().forANote(note).please();

      String body = firstDeveloperMessageBody(conversation);

      assertTrue(body.contains("## Focus Note"));
      assertTrue(body.contains("Notebook:"));
      assertTrue(body.contains("Content:"));
      assertTrue(body.contains(note.getContent()));
    }

    @Test
    void shouldIncludeUserAndAssistantMessages() {
      User user = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(user).please();
      Conversation conversation = makeMe.aConversation().forANote(note).from(user).please();

      makeMe.aConversationMessage(conversation).sender(user).message("First question").please();
      makeMe.aConversationMessage(conversation).sender(null).message("AI response").please();
      makeMe.aConversationMessage(conversation).sender(user).message("Follow up question").please();

      List<ResponseInputItem> items = inputItems(conversation);

      assertEquals(5, items.size());
      assertEquals(EasyInputMessage.Role.DEVELOPER, easyInput(items, 0).role());
      assertEquals(EasyInputMessage.Role.DEVELOPER, easyInput(items, 1).role());
      assertTrue(
          easyInputText(items, 1).contains("Make tool calls when user asks to update the note."));
      assertEquals(EasyInputMessage.Role.USER, easyInput(items, 2).role());
      assertEquals("First question", easyInputText(items, 2));
      assertEquals(EasyInputMessage.Role.ASSISTANT, easyInput(items, 3).role());
      assertEquals("AI response", easyInputText(items, 3));
      assertEquals(EasyInputMessage.Role.USER, easyInput(items, 4).role());
      assertEquals("Follow up question", easyInputText(items, 4));
    }

    @Test
    void firstDeveloperMessageDoesNotIncludeNotebookIndexQuestionGenerationInstruction() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe
          .theNotebook(notebook)
          .indexContent("---\nquestion_generation_instruction: NOT_FOR_CHAT\n---\n")
          .please();
      Note note = makeMe.aNote().notebook(notebook).please();
      Conversation conversation = makeMe.aConversation().forANote(note).from(user).please();

      assertFalse(firstDeveloperMessageBody(conversation).contains("NOT_FOR_CHAT"));
    }

    @Test
    void shouldHandleEmptyConversation() {
      Note note = makeMe.aNote().please();
      Conversation conversation = makeMe.aConversation().forANote(note).please();

      List<ResponseInputItem> items = inputItems(conversation);

      assertEquals(2, items.size());
      assertEquals(EasyInputMessage.Role.DEVELOPER, easyInput(items, 0).role());
      assertEquals(EasyInputMessage.Role.DEVELOPER, easyInput(items, 1).role());
    }

    private ConversationAiRequestBuilder builder() {
      return new ConversationAiRequestBuilder(
          focusContextRetrievalService, focusContextMarkdownRenderer);
    }

    private List<ResponseInputItem> inputItems(Conversation conversation) {
      return builder()
          .buildResponseCreateParams(conversation, "gpt-4.1-mini")
          .input()
          .flatMap(i -> i.response())
          .orElseThrow();
    }

    private String firstDeveloperMessageBody(Conversation conversation) {
      return easyInputText(inputItems(conversation), 0);
    }

    private EasyInputMessage easyInput(List<ResponseInputItem> items, int index) {
      return items.get(index).easyInputMessage().orElseThrow();
    }

    private String easyInputText(List<ResponseInputItem> items, int index) {
      return easyInput(items, index).content().asTextInput();
    }
  }
}
