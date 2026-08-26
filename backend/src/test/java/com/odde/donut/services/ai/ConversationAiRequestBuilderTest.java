package com.odde.donut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.services.focusContext.FocusContextConstants;
import com.odde.donut.testability.MakeMe;
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
  com.odde.donut.services.focusContext.FocusContextRetrievalService focusContextRetrievalService;

  @Autowired
  com.odde.donut.services.focusContext.FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  @Nested
  class BuildParams {

    @Test
    void firstDeveloperMessageIncludesFocusContext() {
      Note note = makeMe.aNote().content("description").please();
      Conversation conversation = makeMe.aConversation().forANote(note).please();

      String body = firstDeveloperMessageBody(conversation);

      assertThat(body, containsString(note.getTitle()));
      assertThat(body, containsString(FocusContextConstants.FOCUS_CONTEXT_OPEN_MARKER));
      assertThat(body, containsString(FocusContextConstants.FOCUS_NOTE_OPEN_MARKER));
      assertThat(body, not(containsString("## Focus Note")));
      assertThat(body, containsString("Notebook:"));
      assertThat(body, containsString("Content:"));
      assertThat(body, containsString(note.getContent()));
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

      assertThat(items, hasSize(5));
      assertThat(easyInput(items, 0).role(), equalTo(EasyInputMessage.Role.DEVELOPER));
      assertThat(easyInput(items, 1).role(), equalTo(EasyInputMessage.Role.DEVELOPER));
      assertThat(
          easyInputText(items, 1),
          containsString("Make tool calls when user asks to update the note."));
      assertThat(easyInput(items, 2).role(), equalTo(EasyInputMessage.Role.USER));
      assertThat(easyInputText(items, 2), equalTo("First question"));
      assertThat(easyInput(items, 3).role(), equalTo(EasyInputMessage.Role.ASSISTANT));
      assertThat(easyInputText(items, 3), equalTo("AI response"));
      assertThat(easyInput(items, 4).role(), equalTo(EasyInputMessage.Role.USER));
      assertThat(easyInputText(items, 4), equalTo("Follow up question"));
    }

    @Test
    void firstDeveloperMessageDoesNotIncludeNotebookIndexQuestionGenerationInstruction() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe
          .theNotebook(notebook)
          .readmeContent("---\nquestion_generation_instruction: NOT_FOR_CHAT\n---\n")
          .please();
      Note note = makeMe.aNote().notebook(notebook).please();
      Conversation conversation = makeMe.aConversation().forANote(note).from(user).please();

      assertThat(firstDeveloperMessageBody(conversation), not(containsString("NOT_FOR_CHAT")));
    }

    @Test
    void shouldHandleEmptyConversation() {
      Note note = makeMe.aNote().please();
      Conversation conversation = makeMe.aConversation().forANote(note).please();

      List<ResponseInputItem> items = inputItems(conversation);

      assertThat(items, hasSize(2));
      assertThat(easyInput(items, 0).role(), equalTo(EasyInputMessage.Role.DEVELOPER));
      assertThat(easyInput(items, 1).role(), equalTo(EasyInputMessage.Role.DEVELOPER));
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
