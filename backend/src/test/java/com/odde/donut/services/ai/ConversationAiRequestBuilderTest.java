package com.odde.donut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.services.focusContext.FocusContextConstants;
import com.odde.donut.testability.SpringTestBase;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.FunctionTool;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.Tool;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConversationAiRequestBuilderTest extends SpringTestBase {
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

      assertThat(
          developerMessageTexts(items),
          hasItem("Make tool calls when user asks to update the note."));
      List<EasyInputMessage> exchange = conversationMessages(items);
      assertThat(
          exchange.stream().map(EasyInputMessage::role).toList(),
          contains(
              EasyInputMessage.Role.USER,
              EasyInputMessage.Role.ASSISTANT,
              EasyInputMessage.Role.USER));
      assertThat(
          exchange.stream().map(message -> message.content().asTextInput()).toList(),
          contains("First question", "AI response", "Follow up question"));
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

      assertThat(conversationMessages(items), empty());
    }

    @Test
    void aNoteConversationSearchesTheWebBeforeProposingAContentCorrection() {
      Conversation conversation = makeMe.aConversation().forANote(makeMe.aNote().please()).please();

      assertSearchesTheWebForASource(conversation);
    }

    @Test
    void aRecallConversationSearchesTheWebBeforeProposingAContentCorrection() {
      Conversation conversation =
          makeMe
              .aConversation()
              .forARecallPrompt(
                  makeMe
                      .aRecallPrompt()
                      .forMemoryTracker(makeMe.aMemoryTrackerFor(makeMe.aNote().please()).please())
                      .please())
              .please();

      assertSearchesTheWebForASource(conversation);
    }

    @Test
    void theContentCorrectionToolTakesAnOptionalQuotedSourceAndItsAddress() {
      Conversation conversation = makeMe.aConversation().forANote(makeMe.aNote().please()).please();

      FunctionTool contentTool =
          params(conversation).tools().orElseThrow().stream()
              .filter(Tool::isFunction)
              .map(Tool::asFunction)
              .filter(tool -> tool.name().equals(NoteContentCompletion.class.getSimpleName()))
              .findFirst()
              .orElseThrow();

      assertThat(
          contentTool._parameters().toString(),
          allOf(
              containsString("sourceQuote={type=[string, null]"),
              containsString("sourceUrl={type=[string, null]"),
              containsString("content={type=string"),
              containsString("reason={type=string")));
    }

    private void assertSearchesTheWebForASource(Conversation conversation) {
      ResponseCreateParams params = params(conversation);

      assertThat(params.tools().orElseThrow().stream().filter(Tool::isWebSearch).count(), is(1L));
      assertThat(params.maxToolCalls().orElseThrow(), is(5L));
      assertThat(
          developerMessageTexts(params.input().flatMap(i -> i.response()).orElseThrow()),
          hasItem(
              "Before you propose a correction to the note content, search the web for a source for the corrected claim. If the note has a url property, open that page first. In the tool call give sourceQuote, one passage copied word for word from the source page in the page's own language, and sourceUrl, the address of that page. When you find no source that supports the correction, leave both out."));
    }

    private ConversationAiRequestBuilder builder() {
      return new ConversationAiRequestBuilder(
          focusContextRetrievalService, focusContextMarkdownRenderer);
    }

    private ResponseCreateParams params(Conversation conversation) {
      return builder().buildResponseCreateParams(conversation, "gpt-4.1-mini");
    }

    private List<ResponseInputItem> inputItems(Conversation conversation) {
      return params(conversation).input().flatMap(i -> i.response()).orElseThrow();
    }

    private List<EasyInputMessage> conversationMessages(List<ResponseInputItem> items) {
      return easyInputs(items).stream()
          .filter(message -> !message.role().equals(EasyInputMessage.Role.DEVELOPER))
          .toList();
    }

    private List<String> developerMessageTexts(List<ResponseInputItem> items) {
      return easyInputs(items).stream()
          .filter(message -> message.role().equals(EasyInputMessage.Role.DEVELOPER))
          .map(message -> message.content().asTextInput())
          .toList();
    }

    private List<EasyInputMessage> easyInputs(List<ResponseInputItem> items) {
      return items.stream().map(item -> item.easyInputMessage().orElseThrow()).toList();
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
